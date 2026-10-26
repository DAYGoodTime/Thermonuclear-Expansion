Development prototype; values subject to change
{gray:{hr}}
{aqua:Steam power generation}
Generation (EU/t) = Q x E x R
Q: actual steam intake (L/t); R: startup multiplier this tick ({gold:0-1}); no extra configured power multiplier
E (EU/L): ordinary steam {gold:{var:steam_energy}}; superheated steam {gold:{var:superheated_energy}}
E (EU/L): supercritical steam {gold:{var:supercritical_energy}}; ultra-supercritical steam {gold:{var:ultra_supercritical_energy}}
Low-pressure shafts accept ordinary/superheated/supercritical steam; high-pressure shafts also accept ultra-supercritical steam
{gray:{hr}}
{aqua:Generation mechanics and controls}
Warmup time: {gold:{var:startup_ticks} ticks}; turbine efficiency reaches zero after {gold:{var:decay_ticks} ticks} of shutdown
Wired output is limited by dynamos; excess power is discarded without reducing steam intake; wireless output bypasses dynamo limits
Returns {gold:1 L} distilled water per {gold:{var:steam_per_water} L} steam; fractional water carries across batches; blocked output stops work
GUI steam cap applies per {gold:{var:cycle_ticks}-tick} cycle from the next cycle; {gold:0} stops steam intake
{gray:{hr}}
{aqua:Wireless output upgrade}: right-click with this mod's Wireless Upgrade Chip; retained when dismantled
No dynamos: wireless output to the controller owner's GT network; installed dynamos: wired output
{green:Supports Multi-Amp Dynamos and Laser Dynamos}
