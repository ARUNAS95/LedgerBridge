import ganache from 'ganache';
// Public, disposable development mnemonic. Never fund these accounts on a real network.
const server=ganache.server({wallet:{mnemonic:'test test test test test test test test test test test junk'},chain:{chainId:1337,hardfork:'shanghai'},logging:{quiet:true}});
await server.listen(8545,'127.0.0.1');
console.log('Local Ethereum RPC: http://127.0.0.1:8545 (chain 1337)');
for(const signal of ['SIGINT','SIGTERM']) process.on(signal,async()=>{await server.close();process.exit(0);});
