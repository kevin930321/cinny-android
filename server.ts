import express from 'express';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import JSZip from 'jszip';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const PROJECT_ROOT = path.resolve(__dirname, 'cinny-android');
const PORT = Number(process.env.PORT || 3000);

interface ProjectFileEntry {
  path: string;
  name: string;
  language: string;
  sizeBytes: number;
  lines: number;
}

function detectLanguage(filePath: string): string {
  if (filePath.endsWith('.rs')) return 'rust';
  if (filePath.endsWith('.kt') || filePath.endsWith('.kts')) return 'kotlin';
  if (filePath.endsWith('.toml')) return 'toml';
  if (filePath.endsWith('.xml')) return 'xml';
  if (filePath.endsWith('.sh')) return 'bash';
  if (filePath.endsWith('.yml') || filePath.endsWith('.yaml')) return 'yaml';
  if (filePath.endsWith('.md')) return 'markdown';
  return 'text';
}

function walkProjectFiles(dir: string, baseDir: string, acc: ProjectFileEntry[]): void {
  if (!fs.existsSync(dir)) return;
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  entries.sort((a, b) => {
    if (a.isDirectory() && !b.isDirectory()) return -1;
    if (!a.isDirectory() && b.isDirectory()) return 1;
    return a.name.localeCompare(b.name);
  });

  for (const entry of entries) {
    if (entry.name === 'target' || entry.name === '.gradle' || entry.name === 'build') {
      continue;
    }
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      walkProjectFiles(fullPath, baseDir, acc);
    } else if (entry.isFile()) {
      const relPath = path.relative(baseDir, fullPath).replace(/\\/g, '/');
      const content = fs.readFileSync(fullPath, 'utf-8');
      acc.push({
        path: relPath,
        name: entry.name,
        language: detectLanguage(relPath),
        sizeBytes: Buffer.byteLength(content, 'utf-8'),
        lines: content.split('\n').length,
      });
    }
  }
}

function resolveSafeProjectPath(requestedPath: string): string | null {
  const normalized = requestedPath.replace(/^\/+/, '');
  const resolved = path.resolve(PROJECT_ROOT, normalized);
  if (!resolved.startsWith(PROJECT_ROOT + path.sep)) {
    return null;
  }
  return resolved;
}

async function startServer() {
  const app = express();
  app.use(express.json());

  app.get('/api/project/tree', (_req, res) => {
    try {
      const files: ProjectFileEntry[] = [];
      walkProjectFiles(PROJECT_ROOT, PROJECT_ROOT, files);
      res.json({
        rootName: 'cinny-android',
        fileCount: files.length,
        totalLines: files.reduce((sum, f) => sum + f.lines, 0),
        files,
      });
    } catch (error) {
      res.status(500).json({ error: 'Failed to scan Rust Android project tree' });
    }
  });

  app.get('/api/project/file', (req, res) => {
    const requested = typeof req.query.path === 'string' ? req.query.path : '';
    if (!requested) {
      res.status(400).json({ error: 'Missing path query parameter' });
      return;
    }
    const safePath = resolveSafeProjectPath(requested);
    if (!safePath || !fs.existsSync(safePath) || !fs.statSync(safePath).isFile()) {
      res.status(404).json({ error: 'Project file not found' });
      return;
    }
    const content = fs.readFileSync(safePath, 'utf-8');
    res.json({
      path: requested.replace(/^\/+/, ''),
      language: detectLanguage(safePath),
      content,
    });
  });

  app.get('/api/project/download.zip', async (_req, res) => {
    try {
      const files: ProjectFileEntry[] = [];
      walkProjectFiles(PROJECT_ROOT, PROJECT_ROOT, files);
      const zip = new JSZip();
      for (const file of files) {
        const fullPath = path.join(PROJECT_ROOT, file.path);
        const content = fs.readFileSync(fullPath);
        zip.file(`cinny-android/${file.path}`, content);
      }
      const buffer = await zip.generateAsync({ type: 'nodebuffer', compression: 'DEFLATE' });
      res.setHeader('Content-Type', 'application/zip');
      res.setHeader('Content-Disposition', 'attachment; filename="cinny-rust-android.zip"');
      res.send(buffer);
    } catch (error) {
      res.status(500).json({ error: 'Failed to generate ZIP archive' });
    }
  });

  if (process.env.NODE_ENV !== 'production') {
    const { createServer: createViteServer } = await import('vite');
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: 'spa',
    });
    app.use(vite.middlewares);
  } else {
    const distDir = path.resolve(__dirname, 'dist');
    app.use(express.static(distDir));
    app.get('*', (_req, res) => {
      res.sendFile(path.join(distDir, 'index.html'));
    });
  }

  app.listen(PORT, '0.0.0.0', () => {
    console.log(`Cinny Rust Android Studio server listening on http://0.0.0.0:${PORT}`);
  });
}

startServer();
