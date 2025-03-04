// notion_recursive.js

const { NotionToMarkdown } = require("notion-to-md");
const { Client } = require("@notionhq/client");

const notion = new Client({ auth: process.argv[2] });
// Notion client를 명시적으로 전달하도록 수정
const n2m = new NotionToMarkdown({ notionClient: notion });

async function getPageTitle(pageId) {
  try {
    const page = await notion.pages.retrieve({ page_id: pageId });
    // 페이지의 properties에서 첫번째 title 타입의 필드를 제목으로 사용합니다.
    for (const key in page.properties) {
      const property = page.properties[key];
      if (property.type === "title") {
        return property.title.map((t) => t.plain_text).join("");
      }
    }
    return "Untitled Page";
  } catch (error) {
    console.error("페이지 제목을 가져오는 중 오류 발생:", error);
    return "Untitled Page";
  }
}

async function getAllBlocks(blockId) {
  let blocks = [];
  let cursor;
  
  while (true) {
    const response = await notion.blocks.children.list({
      block_id: blockId,
      page_size: 100,
      start_cursor: cursor,
    });
    
    blocks = blocks.concat(response.results);
    
    if (!response.has_more) break;
    cursor = response.next_cursor;
  }
  
  return blocks;
}

/**
 * Notion 페이지를 재귀적으로 Markdown 변환하고, 페이지 구조를 함께 반환하는 함수
 * @param {string} pageId - 변환할 Notion 페이지 ID
 * @returns {Promise<{ markdown: string, structure: object }>}
 */
async function fetchPageRecursively(pageId) {
  try {
    const title = await getPageTitle(pageId);
    const structure = { id: pageId, title: title, children: [] };

    console.log(`페이지 가져오는 중: ${title} (${pageId})`);

    const mdBlocks = await n2m.pageToMarkdown(pageId);
    let markdownContent = `# ${title}\n\n`;
    
    const mdString = n2m.toMarkdownString(mdBlocks);
    markdownContent += typeof mdString === 'string' ? mdString : mdString.parent;

    // getAllBlocks 함수를 사용하여 모든 블록 가져오기
    const blocks = await getAllBlocks(pageId);
    console.log(`총 ${blocks.length}개의 블록을 발견했습니다.`);

    for (const block of blocks) {
      console.log(`처리 중인 블록: ${block.type} (${block.id})`);
      
      if (block.type === "child_page") {
        const childPageId = block.id;
        console.log(`하위 페이지 발견: ${childPageId}`);
        
        try {
          const childResult = await fetchPageRecursively(childPageId);
          markdownContent += `\n\n---\n\n${childResult.markdown}`;
          structure.children.push(childResult.structure);
        } catch (error) {
          console.error(`하위 페이지 ${childPageId} 처리 중 오류:`, error);
        }
      } 
      else if (block.type === "column_list") {
        console.log('컬럼 리스트 블록 발견:', block.id);
        const columnBlocks = await getAllBlocks(block.id);
        console.log(`컬럼 수: ${columnBlocks.length}`);
        
        for (const column of columnBlocks) {
          console.log(`컬럼 처리 중: ${column.id}`);
          const columnChildren = await getAllBlocks(column.id);
          console.log(`컬럼 내 블록 수: ${columnChildren.length}`);
          
          for (const child of columnChildren) {
            console.log(`컬럼 내 블록 타입: ${child.type}`);
            
            if (child.type === "child_page") {
              console.log(`컬럼 내 하위 페이지 발견: ${child.id}`);
              try {
                const childResult = await fetchPageRecursively(child.id);
                markdownContent += `\n\n---\n\n${childResult.markdown}`;
                structure.children.push(childResult.structure);
              } catch (error) {
                console.error(`컬럼 내 하위 페이지 ${child.id} 처리 중 오류:`, error);
              }
            } else if (child.type === "child_database") {
              console.log(`컬럼 내 데이터베이스 발견: ${child.id}`);
              try {
                let allPages = [];
                let cursor;
                
                while (true) {
                  const response = await notion.databases.query({
                    database_id: child.id,
                    start_cursor: cursor,
                    page_size: 100,
                  });
                  
                  allPages = allPages.concat(response.results);
                  console.log(`데이터베이스에서 ${response.results.length}개의 페이지 발견`);
                  
                  if (!response.has_more) break;
                  cursor = response.next_cursor;
                }
                
                const databaseStructure = {
                  id: child.id,
                  title: child.child_database?.title || "Database",
                  type: "database",
                  children: []
                };
                
                for (const page of allPages) {
                  try {
                    const pageResult = await fetchPageRecursively(page.id);
                    markdownContent += `\n\n---\n\n${pageResult.markdown}`;
                    databaseStructure.children.push(pageResult.structure);
                  } catch (error) {
                    console.error(`데이터베이스 페이지 ${page.id} 처리 중 오류:`, error);
                  }
                }
                
                structure.children.push(databaseStructure);
              } catch (error) {
                console.error(`컬럼 내 데이터베이스 ${child.id} 처리 중 오류:`, error);
              }
            }
          }
        }
      }
      else if (block.type === "child_database") {
        const databaseId = block.id;
        console.log(`데이터베이스 발견: ${databaseId}`);
        
        try {
          let allPages = [];
          let cursor;
          
          // 데이터베이스의 모든 페이지 가져오기
          while (true) {
            const response = await notion.databases.query({
              database_id: databaseId,
              start_cursor: cursor,
              page_size: 100,
            });
            
            allPages = allPages.concat(response.results);
            
            if (!response.has_more) break;
            cursor = response.next_cursor;
          }
          
          const databaseStructure = {
            id: databaseId,
            title: block.child_database.title || "Database",
            type: "database",
            children: []
          };
          
          for (const page of allPages) {
            try {
              const pageResult = await fetchPageRecursively(page.id);
              markdownContent += `\n\n---\n\n${pageResult.markdown}`;
              databaseStructure.children.push(pageResult.structure);
            } catch (error) {
              console.error(`데이터베이스 페이지 ${page.id} 처리 중 오류:`, error);
            }
          }
          
          structure.children.push(databaseStructure);
        } catch (error) {
          console.error(`데이터베이스 ${databaseId} 처리 중 오류:`, error);
        }
      }
    }

    return { markdown: markdownContent, structure: structure };
  } catch (error) {
    console.error(`페이지 ${pageId} 처리 중 에러 발생:`, error);
    throw error;
  }
}

async function fetchStructureOnly(pageId) {
  try {
    const title = await getPageTitle(pageId);
    const structure = { 
      id: pageId, 
      title: title, 
      children: [],
      isChecked: false  // 기본값으로 false 설정
    };

    const blocks = await getAllBlocks(pageId);
    
    for (const block of blocks) {
      if (block.type === "child_page") {
        const childStructure = await fetchStructureOnly(block.id);
        structure.children.push(childStructure);
      } 
      else if (block.type === "column_list") {
        const columnBlocks = await getAllBlocks(block.id);
        
        for (const column of columnBlocks) {
          const columnChildren = await getAllBlocks(column.id);
          
          for (const child of columnChildren) {
            if (child.type === "child_page") {
              const childStructure = await fetchStructureOnly(child.id);
              structure.children.push(childStructure);
            } else if (child.type === "child_database") {
              const databaseStructure = {
                id: child.id,
                title: child.child_database?.title || "Database",
                type: "database",
                children: [],
                isChecked: false
              };
              
              const allPages = await getAllDatabasePages(child.id);
              for (const page of allPages) {
                const pageStructure = await fetchStructureOnly(page.id);
                databaseStructure.children.push(pageStructure);
              }
              
              structure.children.push(databaseStructure);
            }
          }
        }
      }
      else if (block.type === "child_database") {
        const databaseStructure = {
          id: block.id,
          title: block.child_database.title || "Database",
          type: "database",
          children: [],
          isChecked: false
        };
        
        const allPages = await getAllDatabasePages(block.id);
        for (const page of allPages) {
          const pageStructure = await fetchStructureOnly(page.id);
          databaseStructure.children.push(pageStructure);
        }
        
        structure.children.push(databaseStructure);
      }
    }

    return structure;
  } catch (error) {
    console.error(`구조 생성 중 에러 발생: ${error}`);
    throw error;
  }
}

async function generateMarkdownFromStructure(structure, depth = 0, parent = null) {
  let markdown = '';
  const indent = '  '.repeat(depth);
  
  structure.parent = parent;
  
  if (structure.isChecked) {
    if (depth === 0) {
      markdown += '{\n';
    } else {
      markdown += `${indent}{\n`;
    }
    
    markdown += `${indent}  "title": "${structure.title.replace(/"/g, '\\"')}",\n`;
    markdown += `${indent}  "pageid": "${structure.id}",\n`;
    // type 추가 - database가 아니면 "page"로 기본값 설정
    markdown += `${indent}  "type": "${structure.type || 'page'}"`;
    
    try {
      const mdBlocks = await n2m.pageToMarkdown(structure.id);
      const mdString = n2m.toMarkdownString(mdBlocks);
      const content = typeof mdString === 'string' ? mdString : mdString.parent;
      
      if (content && content.trim()) {
        // 하위 페이지 제목과 관련된 내용 제거
        const contentLines = content.split('\n')
          .map(line => line.trim())
          .filter(line => line.length > 0)
          // ## 로 시작하는 하위 페이지 제목 줄과 그 다음 줄(설명) 제거
          .filter((line, index, array) => {
            if (line.startsWith('##')) {
              array[index + 1] = ''; // 다음 줄도 제거하기 위해 빈 문자열로 설정
              return false;
            }
            return true;
          })
          .filter(line => line.length > 0); // 빈 줄 다시 한번 제거
        
        const formattedContent = contentLines
          .join('\\n')
          .replace(/"/g, '\\"')
          .replace(/\r/g, '');
        
        if (formattedContent.trim()) {
          markdown += `,\n${indent}  "content": "${formattedContent}"`;
        }
      }
    } catch (error) {
      console.error(`페이지 ${structure.title} 내용 가져오기 실패:`, error);
    }
    
    if (structure.children && structure.children.length > 0) {
      const checkedChildren = structure.children.filter(child => child.isChecked);
      
      if (checkedChildren.length > 0) {
        markdown += `,\n${indent}  "children": [\n`;
        
        for (let i = 0; i < checkedChildren.length; i++) {
          const childContent = await generateMarkdownFromStructure(
            checkedChildren[i],
            depth + 2,
            structure
          );
          
          if (childContent.trim()) {
            markdown += childContent;
            if (i < checkedChildren.length - 1) {
              markdown += ',\n';
            }
          }
        }
        
        markdown += `\n${indent}  ]`;
      }
    }
    
    markdown += `\n${indent}}`;
  }
  
  return markdown;
}

// 하위 항목 중에 체크된 항목이 있는지 확인하는 헬퍼 함수
function hasCheckedDescendants(structure) {
  if (structure.isChecked) {
    return true;
  }
  
  if (structure.children && structure.children.length > 0) {
    return structure.children.some(child => hasCheckedDescendants(child));
  }
  
  return false;
}

async function main() {
    const notionToken = process.argv[2];  // 첫 번째 인자: Notion 토큰
    const pageId = process.argv[3];       // 두 번째 인자: 페이지 ID
    const menuId = process.argv[4];       // 세 번째 인자: 메뉴 ID (1 또는 2)
    
    if (!notionToken || !pageId || !menuId) {
        console.error('Notion token, page ID, and menu ID are required');
        process.exit(1);
    }

    try {
        // Notion 클라이언트 초기화
        const notion = new Client({ auth: notionToken });
        const n2m = new NotionToMarkdown({ notionClient: notion });

        if (menuId === '1') {
            const structure = await fetchStructureOnly(pageId);
            console.log(JSON.stringify(structure));
        } 
        else if (menuId === '2') {
            if (!process.argv[5]) {
                console.error("구조 데이터가 필요합니다.");
                process.exit(1);
            }

            try {
                // 디버깅을 위해 받은 구조 데이터 출력
                //console.error("Received structure data:", process.argv[5]);
                
                // 문자열에서 따옴표 제거 및 파싱
                let structureStr = process.argv[5];
                if (structureStr.startsWith('"') && structureStr.endsWith('"')) {
                    structureStr = structureStr.slice(1, -1);
                }
                
                // 이스케이프된 문자열을 다시 원래대로 복원
                structureStr = structureStr
                    .replace(/\\"/g, '"')
                    .replace(/\\n/g, '\n')
                    .replace(/\\r/g, '\r');
                
                const structure = JSON.parse(structureStr);
                const markdown = await generateMarkdownFromStructure(structure);
                
                if (markdown.trim()) {
                    console.log(markdown);
                } else {
                    console.error("생성된 마크다운이 비어있습니다. isChecked가 true인 페이지가 있는지 확인해주세요.");
                    process.exit(1);
                }
            } catch (parseError) {
                console.error("구조 데이터 파싱 중 오류 발생:", parseError);
                console.error("전달받은 데이터:", process.argv[5]);
                process.exit(1);
            }
        } 
        else {
            console.error("잘못된 메뉴 ID입니다. 1 또는 2를 입력해주세요.");
            process.exit(1);
        }
        
        process.exit(0);
    } catch (error) {
        console.error(error);
        process.exit(1);
    }
}

// 데이터베이스의 모든 페이지를 가져오는 헬퍼 함수
async function getAllDatabasePages(databaseId) {
  let allPages = [];
  let cursor;
  
  while (true) {
    const response = await notion.databases.query({
      database_id: databaseId,
      start_cursor: cursor,
      page_size: 100,
    });
    
    allPages = allPages.concat(response.results);
    
    if (!response.has_more) break;
    cursor = response.next_cursor;
  }
  
  return allPages;
}

main();