import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import ganache from 'ganache';
import {BrowserProvider,ContractFactory,id,ZeroHash,ZeroAddress} from 'ethers';
import './compile.mjs';
const artifact=JSON.parse(fs.readFileSync('deployment/contract.json'));
async function setup(){const rpc=ganache.provider({logging:{quiet:true},chain:{hardfork:'shanghai'}});const p=new BrowserProvider(rpc);p.pollingInterval=20;const owner=await p.getSigner(0);const other=await p.getSigner(1);const c=await new ContractFactory(artifact.abi,artifact.bytecode,owner).deploy(await owner.getAddress());await c.waitForDeployment();return{rpc,owner,other,c};}
for(const [name,run] of [
 ['authorized writer anchors a digest',async({c})=>{await(await c.anchor(id('record'),id('digest'))).wait();assert.equal(await c.records(id('record')),id('digest'));}],
 ['identical redelivery does not emit a second anchor',async({c})=>{await(await c.anchor(id('record'),id('digest'))).wait();const r=await(await c.anchor(id('record'),id('digest'))).wait();assert.equal(r.logs.length,0);}],
 ['conflicting digest cannot overwrite an anchor',async({c})=>{await(await c.anchor(id('record'),id('digest'))).wait();await assert.rejects(c.anchor(id('record'),id('different')));assert.equal(await c.records(id('record')),id('digest'));}],
 ['unauthorized wallet cannot write',async({c,other})=>{await assert.rejects(c.connect(other).anchor(id('record'),id('digest')));}],
 ['zero record id and digest are rejected',async({c})=>{await assert.rejects(c.anchor(ZeroHash,id('digest')));await assert.rejects(c.anchor(id('record'),ZeroHash));}],
 ['zero writer cannot be configured',async({owner})=>{await assert.rejects(new ContractFactory(artifact.abi,artifact.bytecode,owner).deploy(ZeroAddress));}]
]) test(name,async()=>{const x=await setup();try{await run(x);}finally{await x.rpc.disconnect();}});
