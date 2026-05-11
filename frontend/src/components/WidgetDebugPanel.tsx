interface Props {
  hasMounted: boolean;
  hasPortalTarget: boolean;
  bubbleInDom: boolean;
  isOpen: boolean;
  sessionId: string;
}

export function WidgetDebugPanel({ hasMounted, hasPortalTarget, bubbleInDom, isOpen, sessionId }: Props) {
  return (
    <aside
      style={{
        position: 'fixed',
        top: 16,
        left: 16,
        zIndex: 2147483647,
        width: 280,
        padding: '12px 14px',
        borderRadius: 12,
        background: 'rgba(17, 24, 39, 0.92)',
        color: '#fff',
        fontSize: 12,
        lineHeight: 1.5,
        boxShadow: '0 12px 32px rgba(0, 0, 0, 0.25)',
        fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace',
      }}
      aria-label="Painel de diagnóstico do widget"
    >
      <strong style={{ display: 'block', marginBottom: 8 }}>Diagnóstico do widget</strong>
      <div>mounted: {hasMounted ? 'true' : 'false'}</div>
      <div>portalTarget: {hasPortalTarget ? 'document.body' : 'indisponível'}</div>
      <div>bubbleInDom: {bubbleInDom ? 'true' : 'false'}</div>
      <div>dialogOpen: {isOpen ? 'true' : 'false'}</div>
      <div style={{ marginTop: 8, wordBreak: 'break-all' }}>sessionId: {sessionId}</div>
    </aside>
  );
}

