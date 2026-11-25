import fs from 'node:fs';
import path from 'node:path';
import {pathToFileURL} from 'node:url';
import {execFileSync} from 'node:child_process';
const root=path.resolve('src/main/resources/static');
let pages=0;
function inspect(folder){
 for(const entry of fs.readdirSync(folder,{withFileTypes:true})){
  const file=path.join(folder,entry.name);
  if(entry.isDirectory())inspect(file);
  else if(file.endsWith('.html')){
   pages++;
   const text=fs.readFileSync(file,'utf8');
   for(const match of text.matchAll(/(?:src|href)="(\/[^"?#]+)(?:[?#][^"]*)?"/g)){
    if(!fs.existsSync(path.join(root,match[1])))throw new Error('Missing resource in '+file+': '+match[1]);
   }
   if(!text.includes('lang="en"'))throw new Error('Missing English document language: '+file);
  }
 }
}
inspect(root);
const modules=[];
function inspectModules(folder){
 for(const item of fs.readdirSync(folder,{withFileTypes:true})){
  const file=path.join(folder,item.name);
  if(item.isDirectory())inspectModules(file);
  else if(file.endsWith('.js'))modules.push(file);
 }
}
inspectModules(path.join(root,'js'));
for(const file of modules){
 execFileSync(process.execPath,['--check',file],{stdio:'inherit'});
 if(path.basename(file)!=='Application.js')await import(pathToFileURL(file));
}
console.log('Verified '+pages+' HTML pages, their local references, and '+modules.length+' class modules.');
