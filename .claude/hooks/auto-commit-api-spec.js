/**
 * Claude Code PostToolUse 훅
 * docs/api/ 경로의 파일이 Write 또는 Edit 될 때 자동으로 git commit 수행
 */
let input = '';
process.stdin.on('data', chunk => input += chunk);
process.stdin.on('end', () => {
  try {
    const payload = JSON.parse(input);
    const filePath = (payload.tool_input || {}).file_path || '';

    if (filePath.includes('docs/api/')) {
      const { execSync } = require('child_process');
      execSync(
        `git add "${filePath}" && git commit -m "docs: API 명세서 업데이트"`,
        {
          stdio: 'inherit',
          cwd: 'C:/workspace/ESTAID/ESTAID-backend',
          shell: 'bash'
        }
      );
    }
  } catch (e) {
    // 에러는 무시 (커밋할 변경 없음 등)
  }
});
