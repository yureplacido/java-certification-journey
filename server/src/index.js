import http from 'http';
import fs from 'fs';
import path from 'path';
import yaml from 'js-yaml';

const PROJECT_ROOT = '/home/placido/desarrollo/java-certification-journey';
const API_PORT = 3000;

// Carregar arquivos YAML da raiz do projeto
const progress = yaml.load(
  fs.readFileSync(`${PROJECT_ROOT}/docs/progress.yaml`, 'utf-8')
);
const studyPlan = yaml.load(
  fs.readFileSync(`${PROJECT_ROOT}/docs/study-plan.yaml`, 'utf-8')
);
const studyState = yaml.load(
  fs.readFileSync(`${PROJECT_ROOT}/docs/study-state.yaml`, 'utf-8')
);

// Helper seguro para acessar caminhos aninhados
function get(obj, path, defaultValue = null) {
  return path.reduce((o, p) => (o && o[p] !== undefined ? o[p] : defaultValue), obj);
}

function readFileSafe(relativePath) {
  const fullPath = path.resolve(PROJECT_ROOT, relativePath);
  if (!fullPath.startsWith(PROJECT_ROOT + path.sep) && fullPath !== PROJECT_ROOT) {
    throw new Error('Path traversal detected');
  }
  if (!fs.existsSync(fullPath)) {
    throw new Error('File not found: ' + relativePath);
  }
  return fs.readFileSync(fullPath, 'utf-8');
}

function parseYamlFile(relativePath) {
  const content = readFileSafe(relativePath);
  return yaml.load(content);
}

// DTO: Hoje estudo
function getTodayDTO() {
  const diag = get(progress, ['exames', 'diagnostic']) || {};
  const correct = diag.correct || 0;
  const scorePercent = diag.scorePercent !== undefined ? diag.scorePercent : 0;
  const passing = diag.passing !== undefined ? diag.passing : false;

  const activities = studyState.current.activities
    ? Object.entries(studyState.current.activities).map(([key, status]) => ({
        id: key,
        status: status === true || status === 'completed' || status === 'completed'
          ? 'completed'
          : 'pending',
      }))
    : [];

  // Derivar título do dia baseado no primeiro activity key
  let dayTitle = 'StringBuilder';
  if (studyState.current.activities) {
    const firstKey = Object.keys(studyState.current.activities)[0];
    if (firstKey) {
      dayTitle = firstKey.replace('week-2-day-1-', '') || 'StringBuilder';
    }
  }

  return {
    currentWeek: studyState.current.week,
    currentDay: studyState.current.day,
    dayTitle,
    activities,
    content: [],
    traps: [],
    progress: {
      currentWeek: get(progress, ['meta', 'semana_atual'], 1),
      grade: correct,
      score: `${correct}/50`,
      scorePercent,
      passing,
      totalQuestions: 50,
    },
    examContext: {
      id: get(diag, ['examId'], 'diagnostic'),
      status: get(diag, ['status'], 'in_progress'),
      grade: correct,
      passing,
    },
  };
}

// DTO: Plano de estudo
function getStudyPlanDTO() {
  return {
    weeks: (studyPlan.weeks || []).map((w) => ({
      week: w.week,
      title: w.title,
      days: (w.days || []).map((d) => ({
        day: d.day,
        title: d.title,
        topics: d.topics || [],
        activities: (d.activities || []).map((a) => ({
          id: a.id,
          type: a.type,
          title: a.title,
        })),
        content: d.content || [],
        traps: d.traps || [],
      })),
    })),
  };
}

// DTO: Estado do estudo
function getStudyStateDTO() {
  const activities = studyState.current.activities
    ? Object.entries(studyState.current.activities).map(([key, status]) => ({
        id: key,
        status:
          status === true || status === 'completed' ? 'completed' : 'pending',
      }))
    : [];

  return {
    current: {
      week: studyState.current.week,
      day: studyState.current.day,
    },
    activities,
  };
}

// DTO: Progresso acadêmico
function getProgressDTO() {
  const diag = get(progress, ['exames', 'diagnostic']) || {};
  const correct = diag.correct || 0;
  const total = diag.totalQuestions || 50;
  const scorePercent = diag.scorePercent !== undefined ? diag.scorePercent : Math.round((100 * correct) / total);

  return {
    currentWeek: get(progress, ['meta', 'semana_atual'], 1),
    grade: correct,
    score: `${correct}/${total}`,
    scorePercent,
    passing: diag.passing || false,
    totalQuestions: total,
    sections: diag.sections || {},
    topics: diag.topics || {},
  };
}

// DTO: Conteúdo Markdown
function getContentDTO(filePath) {
  const absolutePath = path.resolve(PROJECT_ROOT, filePath);

  // Path traversal protection: garantir que o caminho resolvido está dentro do project root
  if (!absolutePath.startsWith(PROJECT_ROOT + path.sep) && absolutePath !== PROJECT_ROOT) {
    throw new Error('Path traversal detected');
  }

  if (!fs.existsSync(absolutePath)) {
    throw new Error('Content not found: ' + filePath);
  }

  const markdown = fs.readFileSync(absolutePath, 'utf-8');

  // Extrair título do primeiro heading
  let title = filePath.split('/').pop().replace('.md', '');
  const match = markdown.match(/^# (.+)$/m);
  if (match) {
    title = match[1];
  }

  return {
    path: filePath,
    title,
    markdown,
  };
}

// Roteador de requests
function handleRequest(req, res) {
  const url = new URL(req.url, `http://${req.headers.host}`);
  const pathname = url.pathname;

  // Headers CORS
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  // Responder OPTIONS (preflight)
  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  try {
    if (pathname === '/api/study/today') {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify(getTodayDTO()));
    } else if (pathname === '/api/study/plan') {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify(getStudyPlanDTO()));
    } else if (pathname === '/api/study/state') {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify(getStudyStateDTO()));
    } else if (pathname === '/api/progress') {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify(getProgressDTO()));
    } else if (pathname.startsWith('/api/content/')) {
      const relativePath = pathname.slice('/api/content/'.length);
      const content = getContentDTO(relativePath);
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify(content));
    } else {
      res.writeHead(404, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Not found' }));
    }
  } catch (err) {
    console.error('API error:', err.message);
    try {
      res.writeHead(500, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: err.message }));
    } catch (e) {
      // Ignore - headers may already be sent
    }
  }
}

// Iniciar servidor
const server = http.createServer(handleRequest);

server.listen(API_PORT, () => {
  console.log(`Study API running at http://localhost:${API_PORT}`);
});