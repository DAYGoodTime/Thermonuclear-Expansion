Development prototype; values subject to change
{gray:{hr}}
{aqua:Steam power generation}
Startup requires warmup; generation depends only on the input, and the steam input rate per cycle is set in the GUI
Each steam type has its own generation coefficient K (unit EU/L)
Ordinary steam {gold:{var:steam_energy}}; superheated steam {gold:{var:superheated_energy}}
Supercritical steam {gold:{var:supercritical_energy}}; ultra-supercritical steam {gold:{var:ultra_supercritical_energy}}
Low-pressure shafts accept ordinary/superheated/supercritical steam; high-pressure shafts also accept ultra-supercritical steam and raise K by 50%
Final generation V = input x K x {gold:{var:multiple}}
Returns the full amount of distilled water every cycle at a ratio of {gold:{var:steam_per_water} L} : {gold:1 L}
{gray:{hr}}
{aqua:Wireless output upgrade}: unlocked by installing the Wireless Upgrade Chip
No dynamos: wireless output to the controller owner's GT network automatically
{green:Supports Multi-Amp Dynamos and Laser Dynamos}
