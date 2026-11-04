#!/usr/bin/env python3
"""Measure answers and citations against a running assistant-api (TASK-031, TASK-032).

Sends every golden set question and every negative message to a fresh conversation, parses the SSE stream and
writes one row per evaluated message plus a summary to a JSON file. Standard library only.

Prerequisites: guide 2026.10 ingested (DONE), golden set PUT, RETRIEVAL_STRATEGY=rerank, QUERY_REWRITE_ENABLED=true.
With --threshold-candidates (run with the threshold disabled: RETRIEVAL_MIN_RERANK_SCORE=) the summary gets a
thresholdTable: for every candidate c, how many golden, off-topic and targeted messages score below c.
"""

import argparse
import json
import math
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
NON_WORD = re.compile(r"[^\w\s]|_")
SHORT_MESSAGE_MAX_WORDS = 10
SHORT_QUESTION_MAX_WORDS = 5
REQUEST_TIMEOUT_SECONDS = 330
ANSWER_LISTED_CATEGORIES = {"follow-up-small-talk", "follow-up-question", "follow-up-question-long",
                            "follow-up-independent", "name", "personal-info-with-info"}
PERSONAL_INFO_OPENER = ("Hesabına özel bilgileri (bakiye, limit, borç, başvuru durumu, hesap hareketleri) "
                        "göremiyorum.")
OPENERS = {
    "personal-info": PERSONAL_INFO_OPENER,
    "personal-info-with-info": PERSONAL_INFO_OPENER,
    "other-banks": "Başka bankaların ürünleri ve koşulları hakkında bilgi veremiyorum.",
    "investment-advice": "Yatırım tavsiyesi ya da piyasa tahmini veremiyorum.",
}
OFF_TOPIC_CATEGORIES = {"not-in-guide", "off-topic", "gibberish", "follow-up-independent"}
# Distinctive core of each opener; matched anywhere in the answer so reworded variants ("Ancak hesabınıza ...") count.
OPENER_CORES = {
    OPENERS["personal-info"]: "özel bilgileri bakiye limit borç başvuru durumu hesap hareketleri göremiyorum",
    OPENERS["other-banks"]: "bankaların ürünleri ve koşulları hakkında bilgi veremiyorum",
    OPENERS["investment-advice"]: "yatırım tavsiyesi ya da piyasa tahmini veremiyorum",
}


def turkish_lower(text):
    return text.replace("I", "ı").replace("İ", "i").lower()


def normalize(text):
    return WHITESPACE.sub(" ", turkish_lower(text or "")).strip()


def starts_with_any_opener(answer):
    return opener_of(answer) is not None


def opener_of(answer):
    normalized = normalize(answer)
    return next((opener for opener in OPENERS.values() if normalized.startswith(normalize(opener))), None)


def wrong_opener(category, answer):
    used = opener_of(answer)
    return used is not None and used != OPENERS.get(category)


def words_of(text):
    return WHITESPACE.sub(" ", NON_WORD.sub(" ", turkish_lower(text or ""))).strip()


def openers_anywhere(answer):
    words = f" {words_of(answer)} "
    return [opener for opener, core in OPENER_CORES.items() if f" {core} " in words]


def opener_anywhere(category, answer):
    own = OPENERS.get(category)
    return any(opener != own for opener in openers_anywhere(answer))


def is_short_message(text):
    words = WHITESPACE.sub(" ", NON_WORD.sub(" ", turkish_lower(text))).strip()
    if not words:
        return False
    limit = SHORT_QUESTION_MAX_WORDS if "?" in text else SHORT_MESSAGE_MAX_WORDS
    return len(words.split(" ")) <= limit


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
    if answer_source == "NO_WORDS":
        return "NO_WORDS"
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
        "searchQuery": sources.get("searchQuery") if sources else None,
        "queryRewriteStatus": sources.get("queryRewriteStatus") if sources else None,
        "shortMessage": is_short_message(text.split(">>")[-1].strip()),
        "notFoundAnswer": not_found,
        "citations": len(citations),
        "citedContainsAnswer": cited_contains_answer,
        "citedChunks": cited_chunks,
        "citedHeadings": [chunk["heading"] for chunk in cited_chunks],
        "startsWithOpener": starts_with_opener,
        "wrongOpener": wrong_opener(category, answer),
        "openerAnywhere": opener_anywhere(category, answer),
        "openerWithCitedInfo": bool(opener and starts_with_opener and citations),
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
            "fallen": sum(1 for row in golden if is_fallen(row)),
            "fallenTexts": [{"text": row["text"], "answerSource": row["answerSource"],
                             "topRerankScore": row["topRerankScore"]} for row in golden if is_fallen(row)],
            "wrongOpener": sum(1 for row in golden if row["wrongOpener"]),
            "openerAnywhere": sum(1 for row in golden if row["openerAnywhere"]),
            "openerAnywhereTexts": [row["text"] for row in golden if row["openerAnywhere"]],
            "topRerankScore": score_stats([row["topRerankScore"] for row in golden]),
        }
    if negative:
        categories = {}
        for row in negative:
            entry = categories.setdefault(row["category"], {
                "total": 0, "withCitations": 0, "notFound": 0, "smallTalkNoSearch": 0, "smallTalkLabel": 0,
                "errors": 0, "startsWithOpener": 0, "openerHasCitation": 0, "openerTrailingCitation": 0,
                "streamedOpenerHasCitation": 0, "targetedOk": 0, "wrongOpener": 0, "openerAnywhere": 0, "openerWithCitedInfo": 0, "smallTalk": 0, "noWords": 0, "scores": []})
            entry["total"] += 1
            entry["withCitations"] += row["citations"] > 0
            entry["notFound"] += bool(row["notFoundAnswer"])
            entry["smallTalkNoSearch"] += row["route"] == "NO_SEARCH"
            entry["smallTalkLabel"] += row["route"] == "LABEL"
            entry["errors"] += bool(row["errorCode"])
            entry["smallTalk"] += row["answerSource"] == "SMALL_TALK"
            entry["wrongOpener"] += bool(row["wrongOpener"])
            entry["noWords"] += row["answerSource"] == "NO_WORDS"
            entry["openerAnywhere"] += bool(row["openerAnywhere"])
            entry["openerWithCitedInfo"] += bool(row["openerWithCitedInfo"])
            if row["category"] in ANSWER_LISTED_CATEGORIES:
                entry.setdefault("answers", []).append(
                    {"text": row["text"], "answerSource": row["answerSource"], "route": row["route"],
                     "searchQuery": row["searchQuery"], "notFoundAnswer": row["notFoundAnswer"],
                     "citations": row["citations"], "citedHeadings": row["citedHeadings"], "answer": row["answer"]})
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
            "wrongOpener": sum(1 for row in negative if row["wrongOpener"]),
            "openerAnywhere": sum(1 for row in negative if row["openerAnywhere"]),
            "openerAnywhereTexts": [row["text"] for row in negative if row["openerAnywhere"]],
            "categories": categories,
            "topRerankScore": score_stats([row["topRerankScore"] for row in negative]),
        }
    summary["secondPersonSelfReference"] = sum(1 for row in rows if row["secondPersonSelfReference"])
    short_latencies = [row["latencyMs"] for row in rows if row["shortMessage"] and row["latencyMs"] is not None]
    summary["shortMessageLatencyMs"] = {
        "count": len(short_latencies),
        "median": statistics.median(short_latencies) if short_latencies else None,
        "smallTalkMedian": median_or_none([row["latencyMs"] for row in rows if row["shortMessage"]
                                           and row["answerSource"] == "SMALL_TALK"]),
        "otherMedian": median_or_none([row["latencyMs"] for row in rows if row["shortMessage"]
                                       and row["answerSource"] != "SMALL_TALK"]),
    }
    return summary


def median_or_none(values):
    present = [value for value in values if value is not None]
    return statistics.median(present) if present else None


def is_fallen(row):
    return bool(row["notFoundAnswer"]) or row["answerSource"] == "SMALL_TALK"


def floor_to_half(value):
    return math.floor(value * 2) / 2


def below(rows, candidate):
    return [row for row in rows if row["topRerankScore"] is not None and row["topRerankScore"] < candidate]


def scored_texts(rows, reverse):
    scored = sorted((row for row in rows if row["topRerankScore"] is not None),
                    key=lambda row: row["topRerankScore"], reverse=reverse)
    return [{"text": row["text"], "category": row["category"], "topRerankScore": row["topRerankScore"]}
            for row in scored[:3]]


def threshold_analysis(rows, candidates):
    golden = [row for row in rows if row["set"] == "golden"]
    off_topic = [row for row in rows if row["set"] == "negative" and row["category"] in OFF_TOPIC_CATEGORIES]
    targeted = [row for row in rows if row["set"] == "negative" and row["category"] in OPENERS]
    golden_scores = [row["topRerankScore"] for row in golden if row["topRerankScore"] is not None]
    off_topic_scores = [row["topRerankScore"] for row in off_topic if row["topRerankScore"] is not None]
    golden_min = min(golden_scores) if golden_scores else None
    off_topic_max = max(off_topic_scores) if off_topic_scores else None
    recommended = {}
    if golden_min is not None:
        recommended["A_goldenMinMinusOne"] = floor_to_half(golden_min - 1.0)
        recommended["C_goldenMinMinusHalf"] = floor_to_half(golden_min - 0.5)
        if off_topic_max is not None:
            recommended["B_midpoint"] = floor_to_half((golden_min + off_topic_max) / 2)
    all_candidates = sorted(set(candidates) | set(recommended.values()))
    table = []
    for candidate in all_candidates:
        golden_below = below(golden, candidate)
        off_topic_below = below(off_topic, candidate)
        targeted_below = below(targeted, candidate)
        table.append({
            "candidate": candidate,
            "goldenBelow": len(golden_below),
            "goldenBelowTexts": [row["text"] for row in golden_below],
            "offTopicBelow": len(off_topic_below),
            "offTopicTotal": len(off_topic_scores),
            "offTopicNotBelowTexts": [row["text"] for row in off_topic
                                      if row["topRerankScore"] is not None and row not in off_topic_below],
            "targetedBelow": len(targeted_below),
            "targetedBelowTexts": [row["text"] for row in targeted_below],
        })
    return {
        "goldenLowest": scored_texts(golden, reverse=False),
        "goldenMedian": statistics.median(golden_scores) if golden_scores else None,
        "goldenMin": golden_min,
        "offTopicHighest": scored_texts(off_topic, reverse=True),
        "offTopicMax": off_topic_max,
        "targetedScores": [{"text": row["text"], "category": row["category"],
                            "topRerankScore": row["topRerankScore"]} for row in targeted],
        "recommended": recommended,
        "noScore": {
            "golden": sum(1 for row in golden if row["topRerankScore"] is None),
            "offTopic": sum(1 for row in off_topic if row["topRerankScore"] is None),
            "targeted": sum(1 for row in targeted if row["topRerankScore"] is None),
        },
        "thresholdTable": table,
    }


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
    parser.add_argument("--threshold-candidates", default="",
                        help="comma separated rerank score candidates, e.g. -3,-2,-1,-0.5,0 (threshold disabled run)")
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

    summary = summarize(rows)
    candidates = [float(value) for value in args.threshold_candidates.split(",") if value.strip()]
    if candidates:
        summary["threshold"] = threshold_analysis(rows, candidates)
    result = {"baseUrl": args.base_url, "summary": summary, "rows": rows}
    with open(args.out, "w", encoding="utf-8") as file:
        json.dump(result, file, ensure_ascii=False, indent=2)
    print(json.dumps(result["summary"], ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
