import { Callout } from '../../../shared/components/Callout.tsx'
import type { WorkspaceResponse } from '../model/types.ts'

type WorkspaceCalloutProps = { workspace: WorkspaceResponse }

export function WorkspaceCallout({ workspace }: WorkspaceCalloutProps) {
  return (
    <Callout>
      Çalışma alanı <b>{workspace.name}</b> {`(ws ${workspace.workspaceId}) · embedding modeli`}{' '}
      <b>{`${workspace.embeddingModel}@${workspace.embeddingDimensions}`}</b>. Modeli değiştirmek bütün parçaların yeniden
      embed edilmesi demek; retrieval-api farklı modelle yüklemeyi 409 ile reddeder.
    </Callout>
  )
}
