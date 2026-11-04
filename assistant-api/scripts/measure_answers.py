#!/usr/bin/env python3
"""Measure answers and citations against a running assistant-api (TASK-031).

Sends every golden set question and every negative message to a fresh conversation, parses the SSE stream and
writes one row per evaluated message plus a summary to a JSON file. Standard library only.

Prerequisites: guide 2026.10 ingested (DONE), golden set PUT, RETRIEVAL_STRATEGY=rerank, QUERY_REWRITE_ENABLED=true.
"""

import argparse
import json
import re
import statistics
import sys
import time
import urllib.error
import urllib.request

SECOND_PERSON_SELF_REFERENCE = re.compile(r"asistanı(sınız|sın)\b")
PARAPHRASED_NOT_FOUND = re.compile(r"rehberimizde[^.!?\n]*?bulamad[ıi]m")
CITATION = re.compile(r"\[\d+]")
FIRST_SENTENCE_END = re.compile(r"[.!?]")
LEADING_CITATION = re.compile(r"\s*\[\d+]")
WHITESPACE = re.compile(r"\s+")
REQUEST_TIMEOUT_SECONDS = 330
ANSWER_LISTED_CATEGORIES = {"follow-up-small-talk", "follow-up-question", "follow-up-question-long"}
OPENERS = {
    "personal-info": "Hesabına özel bilgileri (bakiye, limit, borç, başvuru durumu, hesap hareketleri) göremiyorum.",
    "other-banks": "Başka bankaların ürünleri ve koşulları hakkında bilgi veremiyorum.",
    "investment-advice": "Yatırım tavsiyesi ya da piyasa tahmini veremiyorum.",
}


def turkish_lower(text):
    return text.replace("I", "ı").replace("İ", "i").lower()


def normalize(text):
    return WHITESPACE.sub(" ", turkish_lower(text or "")).strip()


def starts_with_any_opener(answer):
    normalized = normalize(answer)
    return any(normalized.startswith(normalize(opener)) for opener in OPENERS.values())


def first_sentence_has_citation(answer):
    text = (answer or "").strip()
    end = FIRST_SENTENCE_END.search(text)
    first_sentence = text if end is None else text[:end.start() + 1]
    return bool(CITATION.search(first_sentence))


def opener_trailing_citation(answer):
    text = (answer or "").strip()
    end = FIRST_SENTENCE_END.search(text)
    return end is not None and bool(LEADING_CITATION.match(text[end.end():]))


def opener_has_citation(answer):
    return first_sentence_has_citation(answer) or opener_trailing_citation(answer)


def post_json(base_url, path, body, accept="application/json"):
    request = urllib.request.Request(
        base_url + path,
        data=json.dumps(body).encode("utf-8"),
        headers={"Content-Type": "application/json", "Accept": accept},
        method="POST",
    )
    return urllib.request.urlopen(request, timeout=REQUEST_TIMEOUT_SECONDS)


def create_conversation(base_url):
    with post_json(base_url, "/api/assistant/conversations", {"channel": "PREVIEW"}) as response:
        return json.loads(response.read().decode("utf-8"))["conversationId"]


def send_message(base_url, conversation_id, text):
    events = []
    started = time.monotonic()
    try:
        response = post_json(base_url, f"/api/assistant/conversations/{conversation_id}/messages", {"text": text},
                             accept="text/event-stream, application/problem+json")
    except urllib.error.HTTPError as error:
        body = error.read().decode("utf-8", "replace")
        try:
            error_code = json.loads(body).get("errorCode")
        except json.JSONDecodeError:
            error_code = f"HTTP_{error.code}"
        return [("error", {"errorCode": error_code})], time.monotonic() - started
    with response:
        event_name = None
        data_lines = []
        for raw_line in response:
            line = raw_line.decode("utf-8").rstrip("\r\n")
            if line == "":
                if event_name is not None or data_lines:
                    payload = "\n".join(data_lines)
                    events.append((event_name or "message", json.loads(payload) if payload else None))
                event_name = None
                data_lines = []
                continue
            if line.startswith(":"):
                continue
            field, _, value = line.partition(":")
            if value.startswith(" "):
                value = value[1:]
            if field == "event":
                event_name = value
            elif field == "data":
                data_lines.append(value)
        if event_name is not None or data_lines:
            payload = "\n".join(data_lines)
            events.append((event_name or "message", json.loads(payload) if payload else None))
    return events, time.monotonic() - started


def route_of(sources, answer_source):
    if answer_source != "SMALL_TALK" or sources is None:
        return None
    return "NO_SEARCH" if sources.get("retrievalStrategy") == "NONE" else "LABEL"


def evaluate(set_name, category, text, events, elapsed_seconds):
    tokens = [data["text"] for name, data in events if name == "token"]
    sources = next((data for name, data in events if name == "sources"), None)
    done = next((data for name, data in events if name == "done"), None)
    error = next((data for name, data in events if name == "error"), None)
    context_chunks = sources.get("contextChunks", []) if sources else []
    citations = sources.get("citations", []) if sources else []
    scores = [chunk["rerankScore"] for chunk in context_chunks if chunk.get("rerankScore") is not None]
    answer = done.get("answer") if done and "answer" in done else "".join(tokens)
    answer_source = done.get("answerSource") if done else None
    cited_contains_answer = any(
        1 <= n <= len(context_chunks) and context_chunks[n - 1].get("containsAnswer") for n in citations)
    cited_chunks = [
        {"n": n, "chunkId": context_chunks[n - 1]["chunkId"], "pageNumber": context_chunks[n - 1]["pageNumber"],
         "heading": context_chunks[n - 1]["heading"]}
        for n in citations if 1 <= n <= len(context_chunks)]
    opener = OPENERS.get(category)
    starts_with_opener = (normalize(answer).startswith(normalize(opener)) if opener
                          else starts_with_any_opener(answer))
    not_found = bool(done.get("notFoundAnswer")) if done else None
    has_opener_citation = opener_has_citation(answer)
    return {
        "set": set_name,
        "category": category,
        "text": text,
        "errorCode": error.get("errorCode") if error else None,
        "answerSource": answer_source,
        "route": route_of(sources, answer_source),
        "retrievalStrategy": sources.get("retrievalStrategy") if sources else None,
        "notFoundAnswer": not_found,
        "citations": len(citations),
        "citedContainsAnswer": cited_contains_answer,
        "citedChunks": cited_chunks,
        "startsWithOpener": starts_with_opener,
        "openerHasCitation": has_opener_citation,
        "openerTrailingCitation": opener_trailing_citation(answer),
        "streamedOpenerHasCitation": opener_has_citation("".join(tokens)),
        "paraphrasedNotFound": bool(PARAPHRASED_NOT_FOUND.search(normalize("".join(tokens)))),
        "targetedOk": (None if opener is None else
                       bool((not_found and not citations) or (starts_with_opener and not has_opener_citation))),
        "containsAnswerInContext": any(chunk.get("containsAnswer") for chunk in context_chunks),
        "topRerankScore": max(scores) if scores else None,
        "answer": answer,
        "streamedText": "".join(tokens),
        "secondPersonSelfReference": bool(SECOND_PERSON_SELF_REFERENCE.search(turkish_lower(answer or ""))),
        "latencyMs": done.get("latencyMs") if done else None,
        "elapsedSeconds": round(elapsed_seconds, 2),
    }


def run_message(base_url, set_name, category, line):
    conversation_id = create_conversation(base_url)
    parts = [part.strip() for part in line.split(">>")]
    for leading in parts[:-1]:
        send_message(base_url, conversation_id, leading)
    events, elapsed = send_message(base_url, conversation_id, parts[-1])
    row = evaluate(set_name, category, line, events, elapsed)
    row["conversationId"] = conversation_id
    return row


def load_negatives(path):
    messages = []
    with open(path, encoding="utf-8") as file:
        for raw_line in file:
            line = raw_line.strip()
            if not line or line.startswith("#"):
                continue
            category, _, text = line.partition("|")
            messages.append((category.strip(), text.strip()))
    return messages


def load_golden(path):
    with open(path, encoding="utf-8") as file:
        return [(question["category"], question["text"]) for question in json.load(file)["questions"]]


def score_stats(values):
    present = [value for value in values if value is not None]
    if not present:
        return {"min": None, "median": None, "max": None, "count": 0}
    return {"min": min(present), "median": statistics.median(present), "max": max(present), "count": len(present)}


def summarize(rows):
    golden = [row for row in rows if row["set"] == "golden"]
    negative = [row for row in rows if row["set"] == "negative"]
    summary = {}
    if golden:
        summary["golden"] = {
            "total": len(golden),
            "errors": sum(1 for row in golden if row["errorCode"]),
            "answered": sum(1 for row in golden if row["notFoundAnswer"] is False and not row["errorCode"]),
            "notFound": sum(1 for row in golden if row["notFoundAnswer"]),
            "withCitations": sum(1 for row in golden if row["citations"] > 0),
            "citedContainsAnswer": sum(1 for row in golden if row["citedContainsAnswer"]),
            "answerSources": count_by(golden, "answerSource"),
            "paraphrasedNotFound": sum(1 for row in golden if row["paraphrasedNotFound"]),
            "goldenStartsWithOpener": sum(1 for row in golden if row["startsWithOpener"]),
            "goldenStartsWithOpenerTexts": [row["text"] for row in golden if row["startsWithOpener"]],
            "topRerankScore": score_stats([row["topRerankScore"] for row in golden]),
        }
    if negative:
        categories = {}
        for row in negative:
            entry = categories.setdefault(row["category"], {
                "total": 0, "withCitations": 0, "notFound": 0, "smallTalkNoSearch": 0, "smallTalkLabel": 0,
                "errors": 0, "startsWithOpener": 0, "openerHasCitation": 0, "openerTrailingCitation": 0,
                "streamedOpenerHasCitation": 0, "targetedOk": 0, "scores": []})
            entry["total"] += 1
            entry["withCitations"] += row["citations"] > 0
            entry["notFound"] += bool(row["notFoundAnswer"])
            entry["smallTalkNoSearch"] += row["route"] == "NO_SEARCH"
            entry["smallTalkLabel"] += row["route"] == "LABEL"
            entry["errors"] += bool(row["errorCode"])
            entry["smallTalk"] = entry.get("smallTalk", 0) + (row["answerSource"] == "SMALL_TALK")
            if row["category"] in ANSWER_LISTED_CATEGORIES:
                entry.setdefault("answers", []).append(
                    {"text": row["text"], "answerSource": row["answerSource"], "route": row["route"],
                     "notFoundAnswer": row["notFoundAnswer"], "citations": row["citations"], "answer": row["answer"]})
            entry["startsWithOpener"] += bool(row["startsWithOpener"])
            entry["openerHasCitation"] += bool(row["openerHasCitation"])
            entry["openerTrailingCitation"] += bool(row["openerTrailingCitation"])
            entry["streamedOpenerHasCitation"] += bool(row["streamedOpenerHasCitation"])
            entry["targetedOk"] += bool(row["targetedOk"])
            entry["scores"].append(row["topRerankScore"])
        for entry in categories.values():
            entry["maxTopRerankScore"] = score_stats(entry.pop("scores"))["max"]
        summary["negative"] = {
            "total": len(negative),
            "withCitations": sum(1 for row in negative if row["citations"] > 0),
            "untargetedWithCitations": sum(
                1 for row in negative if row["category"] not in OPENERS and row["citations"] > 0),
            "targetedOk": sum(1 for row in negative if row["targetedOk"]),
            "targetedTotal": sum(1 for row in negative if row["category"] in OPENERS),
            "categories": categories,
            "topRerankScore": score_stats([row["topRerankScore"] for row in negative]),
        }
    summary["secondPersonSelfReference"] = sum(1 for row in rows if row["secondPersonSelfReference"])
    return summary


def count_by(rows, key):
    counts = {}
    for row in rows:
        counts[str(row[key])] = counts.get(str(row[key]), 0) + 1
    return counts


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--base-url", default="http://localhost:8090")
    parser.add_argument("--golden", default="docs/rehber/golden/rehber-golden-v1.json")
    parser.add_argument("--negatives", default="assistant-api/scripts/negative-messages.txt")
    parser.add_argument("--sets", default="golden,negative", help="comma separated: golden, negative")
    parser.add_argument("--categories", default="", help="comma separated negative categories (default: all)")
    parser.add_argument("--out", required=True)
    args = parser.parse_args()

    sets = {name.strip() for name in args.sets.split(",") if name.strip()}
    work = []
    if "golden" in sets:
        work += [("golden", category, text) for category, text in load_golden(args.golden)]
    if "negative" in sets:
        categories = {name.strip() for name in args.categories.split(",") if name.strip()}
        work += [("negative", category, text) for category, text in load_negatives(args.negatives)
                 if not categories or category in categories]

    rows = []
    for index, (set_name, category, text) in enumerate(work, start=1):
        row = run_message(args.base_url, set_name, category, text)
        rows.append(row)
        print(f"[{index}/{len(work)}] {set_name} {category}: source={row['answerSource']} "
              f"notFound={row['notFoundAnswer']} citations={row['citations']} top={row['topRerankScore']} "
              f"error={row['errorCode']}", file=sys.stderr, flush=True)

    result = {"baseUrl": args.base_url, "summary": summarize(rows), "rows": rows}
    with open(args.out, "w", encoding="utf-8") as file:
        json.dump(result, file, ensure_ascii=False, indent=2)
    print(json.dumps(result["summary"], ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
