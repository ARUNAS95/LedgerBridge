import fs from 'node:fs';
import solc from 'solc';
const input = {language:'Solidity',sources:{'AuditAnchor.sol':{content:fs.readFileSync('contracts/AuditAnchor.sol','utf8')}},settings:{evmVersion:'shanghai',optimizer:{enabled:true,runs:200},outputSelection:{'*':{'*':['abi','evm.bytecode.object']}}}};
const result=JSON.parse(solc.compile(JSON.stringify(input)));
for(const e of result.errors??[]) if(e.severity==='error') throw new Error(e.formattedMessage);
const c=result.contracts['AuditAnchor.sol'].AuditAnchor;
fs.mkdirSync('deployment',{recursive:true});
fs.writeFileSync('deployment/contract.json',JSON.stringify({abi:c.abi,bytecode:'0x'+c.evm.bytecode.object},null,2));
console.log('Compiled AuditAnchor with Solidity',solc.version());
