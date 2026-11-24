import { Download } from 'lucide-react';

export type StudioTab = 'simulator' | 'source' | 'architecture' | 'build';

interface TopNavProps {
  activeTab: StudioTab;
  onSelectTab: (tab: StudioTab) => void;
}

const NAV_ITEMS: Array<{ id: StudioTab; label: string }> = [
  { id: 'simulator', label: '即時模擬器' },
  { id: 'source', label: '程式碼瀏覽' },
  { id: 'architecture', label: '架構對照' },
  { id: 'build', label: '建置指南' },
];

export function TopNav({ activeTab, onSelectTab }: TopNavProps) {
  return (
    <header className="sticky top-0 z-30 border-b border-slate-200 bg-white/95 backdrop-blur-sm">
      <div className="mx-auto flex max-w-7xl items-center justify-between gap-8 px-4 py-3 sm:px-6">
        <button
          type="button"
          onClick={() => onSelectTab('simulator')}
          className="text-left text-lg font-bold tracking-tight text-slate-900 whitespace-nowrap shrink-0 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-600 rounded"
        >
          Cinny Rust Android
        </button>

        <nav
          aria-label="主要功能導覽"
          className="hidden md:flex items-center gap-6"
        >
          {NAV_ITEMS.map((item) => {
            const isActive = activeTab === item.id;
            return (
              <button
                key={item.id}
                type="button"
                onClick={() => onSelectTab(item.id)}
                className={`relative py-2 text-sm font-medium transition-colors whitespace-nowrap shrink-0 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-600 rounded ${
                  isActive
                    ? 'text-indigo-600'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                {item.label}
                {isActive && (
                  <span
                    aria-hidden="true"
                    className="absolute inset-x-0 -bottom-[13px] h-0.5 bg-indigo-600"
                  />
                )}
              </button>
            );
          })}
        </nav>

        <div className="shrink-0">
          <a
            href="/api/project/download.zip"
            download="cinny-rust-android.zip"
            className="inline-flex min-h-[40px] items-center gap-2 rounded-lg bg-slate-900 px-4 py-2 text-xs font-semibold text-white transition-colors hover:bg-slate-800 whitespace-nowrap shrink-0 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-600"
          >
            <Download className="h-3.5 w-3.5" aria-hidden="true" />
            <span>下載專案 ZIP</span>
          </a>
        </div>
      </div>

      {/* Mobile secondary single-row tab strip */}
      <nav
        aria-label="行動端功能導覽"
        className="flex md:hidden border-t border-slate-100 px-2 overflow-x-auto"
      >
        {NAV_ITEMS.map((item) => {
          const isActive = activeTab === item.id;
          return (
            <button
              key={item.id}
              type="button"
              onClick={() => onSelectTab(item.id)}
              className={`min-h-[44px] flex-1 px-3 py-2 text-xs font-medium whitespace-nowrap shrink-0 border-b-2 transition-colors ${
                isActive
                  ? 'border-indigo-600 text-indigo-600'
                  : 'border-transparent text-slate-600 hover:text-slate-900'
              }`}
            >
              {item.label}
            </button>
          );
        })}
      </nav>
    </header>
  );
}
