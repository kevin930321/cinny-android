import { useEffect, useState } from 'react';
import { Check, Copy, FileCode2, FolderTree, Search } from 'lucide-react';

interface ProjectFileEntry {
  path: string;
  name: string;
  language: string;
  sizeBytes: number;
  lines: number;
}

type FileCategory = 'all' | 'rust' | 'android' | 'scripts';

export function SourceExplorer() {
  const [files, setFiles] = useState<ProjectFileEntry[]>([]);
  const [totalLines, setTotalLines] = useState(0);
  const [selectedPath, setSelectedPath] = useState<string>('rust-core/src/client.rs');
  const [fileContent, setFileContent] = useState<string>('');
  const [fileLanguage, setFileLanguage] = useState<string>('rust');
  const [loadingTree, setLoadingTree] = useState(true);
  const [loadingFile, setLoadingFile] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [search, setSearch] = useState('');
  const [category, setCategory] = useState<FileCategory>('all');
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    let active = true;
    setLoadingTree(true);
    fetch('/api/project/tree')
      .then((res) => {
        if (!res.ok) throw new Error('無法載入專案檔案目錄');
        return res.json();
      })
      .then((data) => {
        if (!active) return;
        setFiles(data.files ?? []);
        setTotalLines(data.totalLines ?? 0);
        setLoadingTree(false);
      })
      .catch((err) => {
        if (!active) return;
        setErrorMsg(err.message);
        setLoadingTree(false);
      });
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    if (!selectedPath) return;
    let active = true;
    setLoadingFile(true);
    fetch(`/api/project/file?path=${encodeURIComponent(selectedPath)}`)
      .then((res) => {
        if (!res.ok) throw new Error('無法讀取檔案內容');
        return res.json();
      })
      .then((data) => {
        if (!active) return;
        setFileContent(data.content ?? '');
        setFileLanguage(data.language ?? 'text');
        setLoadingFile(false);
      })
      .catch((err) => {
        if (!active) return;
        setErrorMsg(err.message);
        setLoadingFile(false);
      });
    return () => {
      active = false;
    };
  }, [selectedPath]);

  const filteredFiles = files.filter((f) => {
    const matchesCat =
      category === 'all'
        ? true
        : category === 'rust'
          ? f.path.startsWith('rust-core/')
          : category === 'android'
            ? f.path.startsWith('android/')
            : !f.path.startsWith('rust-core/') && !f.path.startsWith('android/');
    const q = search.trim().toLowerCase();
    const matchesSearch = q === '' || f.path.toLowerCase().includes(q);
    return matchesCat && matchesSearch;
  });

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(fileContent);
      setCopied(true);
      setTimeout(() => setCopied(false), 1600);
    } catch {
      // Clipboard blocked in some headless contexts
    }
  };

  const lines = fileContent.split('\n');

  return (
    <div className="grid grid-cols-1 gap-6 lg:grid-cols-12">
      {/* Left File Sidebar */}
      <aside className="rounded-2xl border border-slate-200 bg-white p-4 lg:col-span-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <FolderTree className="h-4 w-4 text-slate-600" aria-hidden="true" />
            <h2 className="text-sm font-semibold text-slate-900">
              cinny-android 專案原始碼
            </h2>
          </div>
          <span className="text-xs text-slate-500 tabular-nums">
            {files.length} 檔案 · {totalLines} 行
          </span>
        </div>

        {/* Filter Segmented Control */}
        <div className="mt-3 grid grid-cols-4 gap-1 rounded-lg bg-slate-100 p-1">
          {(
            [
              { id: 'all', label: '全部' },
              { id: 'rust', label: 'Rust' },
              { id: 'android', label: 'Kotlin' },
              { id: 'scripts', label: '腳本' },
            ] as const
          ).map((tab) => (
            <button
              key={tab.id}
              type="button"
              onClick={() => setCategory(tab.id)}
              className={`min-h-[34px] rounded-md px-2 text-xs font-medium transition-colors whitespace-nowrap ${
                category === tab.id
                  ? 'bg-white text-slate-900 shadow-xs'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* Search Input */}
        <div className="relative mt-3">
          <Search
            className="pointer-events-none absolute top-1/2 left-3 h-3.5 w-3.5 -translate-y-1/2 text-slate-400"
            aria-hidden="true"
          />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="篩選檔案路徑..."
            aria-label="篩選專案檔案"
            className="min-h-[38px] w-full rounded-lg border border-slate-200 bg-slate-50 pr-3 pl-8 text-xs outline-none focus:border-indigo-500 focus:bg-white"
          />
        </div>

        {loadingTree ? (
          <div className="mt-4 space-y-2">
            {Array.from({ length: 6 }).map((_, i) => (
              <div key={i} className="h-9 rounded-lg bg-slate-100" />
            ))}
          </div>
        ) : errorMsg ? (
          <p className="mt-4 text-xs text-red-600">{errorMsg}</p>
        ) : (
          <ul className="mt-3 max-h-[520px] space-y-1 overflow-y-auto pr-1">
            {filteredFiles.map((file) => {
              const active = file.path === selectedPath;
              const dirPart = file.path.includes('/')
                ? file.path.slice(0, file.path.lastIndexOf('/'))
                : '';
              return (
                <li key={file.path}>
                  <button
                    type="button"
                    onClick={() => setSelectedPath(file.path)}
                    className={`flex w-full items-center justify-between gap-2 rounded-lg px-2.5 py-2 text-left transition-colors ${
                      active
                        ? 'bg-indigo-50 text-indigo-700'
                        : 'text-slate-700 hover:bg-slate-50'
                    }`}
                  >
                    <div className="min-w-0 flex-1">
                      <div className="truncate font-mono text-xs font-semibold">
                        {file.name}
                      </div>
                      {dirPart && (
                        <div className="truncate font-mono text-[10px] text-slate-400">
                          {dirPart}
                        </div>
                      )}
                    </div>
                    <span className="shrink-0 text-[11px] text-slate-400 tabular-nums">
                      {file.lines}L
                    </span>
                  </button>
                </li>
              );
            })}
          </ul>
        )}
      </aside>

      {/* Right Code Viewer */}
      <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white lg:col-span-8">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 px-5 py-3">
          <div className="flex min-w-0 items-center gap-2">
            <FileCode2 className="h-4 w-4 shrink-0 text-slate-500" aria-hidden="true" />
            <span className="truncate font-mono text-xs font-semibold text-slate-900">
              {selectedPath}
            </span>
            <span className="text-xs text-slate-400">
              · {fileLanguage} · {lines.length} 行
            </span>
          </div>

          <button
            type="button"
            onClick={handleCopy}
            className="inline-flex min-h-[36px] items-center gap-1.5 rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-xs font-medium text-slate-700 transition-colors hover:bg-slate-50"
          >
            {copied ? (
              <>
                <Check className="h-3.5 w-3.5 text-emerald-600" aria-hidden="true" />
                <span>已複製</span>
              </>
            ) : (
              <>
                <Copy className="h-3.5 w-3.5" aria-hidden="true" />
                <span>複製程式碼</span>
              </>
            )}
          </button>
        </div>

        <div className="max-h-[600px] overflow-auto bg-slate-950 p-4 text-slate-100">
          {loadingFile ? (
            <div className="text-xs text-slate-400">載入檔案中...</div>
          ) : (
            <pre className="font-mono text-xs leading-6">
              <code>
                {lines.map((line, idx) => (
                  <div key={idx} className="flex">
                    <span className="mr-4 w-8 shrink-0 select-none text-right text-slate-500 tabular-nums">
                      {idx + 1}
                    </span>
                    <span className="whitespace-pre">{line || ' '}</span>
                  </div>
                ))}
              </code>
            </pre>
          )}
        </div>
      </section>
    </div>
  );
}
