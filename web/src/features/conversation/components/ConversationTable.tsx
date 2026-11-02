import { useState } from 'react'
import type { MouseEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { StatusBadge } from '../../../shared/components/StatusBadge.tsx'
import table from '../../../shared/components/table.module.css'
import { formatListTime } from '../../../shared/format/formatDateTime.ts'
import { shortenText } from '../../../shared/format/formatText.ts'
import { FIRST_QUESTION_MAX_LENGTH } from '../model/conversationRules.ts'
import { HANDED_OFF_CHIP } from '../model/conversationTexts.ts'
import { channelLabel, conversationLabel } from '../model/turnLabels.ts'
import { TURN_STATUS_LABELS, turnStatusTone } from '../model/turnStatus.ts'
import type { ConversationSummaryResponse } from '../model/types.ts'
import styles from './ConversationTable.module.css'

type ConversationTableProps = { conversations: ConversationSummaryResponse[] }

export function ConversationTable({ conversations }: ConversationTableProps) {
  const [now] = useState(() => new Date())
  return (
    <div className={table.scroll}>
      <table className={table.table}>
        <thead>
          <tr>
            <th>Konuşma</th>
            <th>İlk soru</th>
            <th>Tur</th>
            <th>Durum</th>
            <th>Kanal</th>
            <th>Saat</th>
          </tr>
        </thead>
        <tbody>
          {conversations.map((conversation) => (
            <ConversationRow key={conversation.conversationId} conversation={conversation} now={now} />
          ))}
        </tbody>
      </table>
    </div>
  )
}

type ConversationRowProps = { conversation: ConversationSummaryResponse; now: Date }

function ConversationRow({ conversation, now }: ConversationRowProps) {
  const navigate = useNavigate()
  const detailPath = `/konusmalar/${conversation.conversationId}`

  function handleRowClick(event: MouseEvent<HTMLTableRowElement>) {
    if (event.target instanceof Element && event.target.closest('a, button') !== null) {
      return
    }
    void navigate(detailPath)
  }

  return (
    // The conversation link is the keyboard path to the detail; a click on the rest of the row is a mouse shortcut like the lab.
    // oxlint-disable-next-line jsx-a11y/click-events-have-key-events, jsx-a11y/no-static-element-interactions
    <tr className={table.clickable} onClick={handleRowClick}>
      <td className={table.num}>
        <Link to={detailPath} className={styles.conversationLink}>
          {conversationLabel(conversation.conversationId)}
        </Link>
      </td>
      <td className={table.text}>{shortenText(conversation.firstQuestion, FIRST_QUESTION_MAX_LENGTH)}</td>
      <td className={table.num}>{conversation.turnCount}</td>
      <td>
        <span className={styles.statusCell}>
          <StatusBadge tone={turnStatusTone(conversation.lastTurnStatus)}>
            {TURN_STATUS_LABELS[conversation.lastTurnStatus]}
          </StatusBadge>
          {conversation.handedOff ? <StatusBadge tone="neutral">{HANDED_OFF_CHIP}</StatusBadge> : null}
        </span>
      </td>
      <td>{channelLabel(conversation.channel)}</td>
      <td className={table.num}>{formatListTime(conversation.createdAt, now)}</td>
    </tr>
  )
}
