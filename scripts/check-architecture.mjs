import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import assert from 'node:assert/strict';

const root=path.resolve('.');
function files(folder,extension){return fs.readdirSync(folder,{withFileTypes:true}).flatMap(item=>item.isDirectory()?files(path.join(folder,item.name),extension):item.name.endsWith(extension)?[path.join(folder,item.name)]:[]);}
function mappings(text){return [...text.matchAll(/@(GetMapping|PostMapping|PutMapping|PatchMapping|DeleteMapping|RequestMapping)\s*\(([^)]*)\)/g)].map(m=>m[1]+m[2].replace(/\s+/g,'')).sort();}
const contract=JSON.parse(fs.readFileSync('docs/api-contract.json','utf8'));
const allJava=files(path.resolve('src/main/java'),'.java');
for(const [name,expected]of Object.entries(contract.controllers)){const found=allJava.filter(f=>path.basename(f)===name);assert.equal(found.length,1,name+' not found');assert.deepEqual(mappings(fs.readFileSync(found[0],'utf8')),expected,name+' routes changed');}
const schema=crypto.createHash('sha256').update(fs.readFileSync('src/main/resources/schema.sql')).digest('hex');
assert.equal(schema,contract.schemaSha256,'Database schema changed');
const staticRoot=path.resolve('src/main/resources/static');
assert.deepEqual(files(staticRoot,'.html').map(f=>path.relative(staticRoot,f).replaceAll('\\','/')).sort(),contract.pages,'HTML page paths changed');
const javaFiles=files(path.resolve('src/main/java'),'.java');
for(const file of javaFiles){
 const text=fs.readFileSync(file,'utf8'),relative=path.relative(root,file).replaceAll('\\','/');
 assert(!/@Autowired|@Value\(/.test(text),relative+' uses field/scattered configuration injection');
 if(/\/(User|Student|Teacher|Admin|Section|Enrollment|AdvisingHold|AdvisingWindow|TeacherCourseChoice|LoginId|TimeSlot)\.java$/.test(relative))assert(!/import org\.springframework|import java\.sql|\.repository\.|\.shared\.db\./.test(text),relative+' domain class depends on infrastructure');
 if(/(Service|Controller)\.java$/.test(relative))assert(!/"(?:SELECT |INSERT INTO |UPDATE |DELETE FROM )/.test(text),relative+' embeds SQL');
 if(relative.endsWith('Controller.java'))assert(!/\bdb\./.test(text.replace(/^import .*$/mg,'')),relative+' accesses repositories directly');
}
const jsFiles=files(path.join(staticRoot,'js'),'.js');
for(const file of jsFiles){const text=fs.readFileSync(file,'utf8');assert(!/export (?:async )?function/.test(text),file+' has procedural exports');if(!file.endsWith('ApiClient.js'))assert(!/\bfetch\(/.test(text),file+' bypasses ApiClient');if(file.includes(path.sep+'pages'+path.sep))assert(!/style=|\.style\.|\.on(?:click|input|change)\s*=/.test(text),file+' has presentation/event logic outside components');}
console.log('Architecture verified: unchanged routes, schema and page paths; '+javaFiles.length+' Java files and '+jsFiles.length+' ES6 class modules.');
