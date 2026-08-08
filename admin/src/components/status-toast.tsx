"use client";

import { X } from "lucide-react";
import { createPortal } from "react-dom";
import { useEffect, useState, useSyncExternalStore, type ReactNode } from "react";

const noopSubscribe = (onStoreChange: () => void) => {
  void onStoreChange;
  return () => {};
};
const mountedSnapshot = () => true;
const serverSnapshot = () => false;

export function StatusToast({ children }: { children: ReactNode }) {
  const mounted = useSyncExternalStore(noopSubscribe, mountedSnapshot, serverSnapshot);
  const [visible, setVisible] = useState(true);

  useEffect(() => {
    const timeout = window.setTimeout(() => setVisible(false), 10_000);
    return () => window.clearTimeout(timeout);
  }, []);

  if (!mounted || !visible) return null;

  return createPortal(
    <div className="status-toast" role="status" aria-live="polite">
      <span className="status-toast-content">{children}</span>
      <button type="button" className="status-toast-dismiss focus-ring" onClick={() => setVisible(false)} aria-label="Dismiss message">
        <X size={16} />
      </button>
    </div>,
    document.body,
  );
}
