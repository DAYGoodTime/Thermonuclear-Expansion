Development prototype; values subject to change
{gray:{hr}}
{aqua:Direct generation mode}
Consumes fuel to produce power
Generation (EU/t) = {gold:E x C x P x N x S x M x eta}
E: base fuel energy per pulse; M: IC2 nuclear multiplier x configured direct multiplier; eta: configured direct efficiency
{gray:{hr}}
{aqua:Heat supply mode}
Consumes fuel to accumulate hull heat; cooling fluids carry it out as hot fluids;
Heat (heat/t) = {gold:C x round[P x (P + 1) / 2 x H] x N x S x M x eta x R / {var:cycle_ticks}}
H: base fuel heat; M: configured heat multiplier; eta: configured heat efficiency; R: average startup multiplier this cycle
Supports IC2 coolant, super coolant and NaK composite coolant; distilled water cooling produces ordinary steam
{gray:{hr}}
{aqua:Features and support}
C: cells per fuel rod; N: rods in the current batch; S: configured fuel cycles per second
Pulses per cell: {gold:P = 1 + floor(C / 2) + reflector pulses}; configured reflector pulses apply above {gold:4} rods per batch, otherwise {gold:0}
Formulas assume normal fuel consumption; near depletion, output uses remaining lifetime. Direct generation applies startup scaling to per-tick output
Fuel rod block tiers {gold:1-4} allow max parallel per batch: {gold:{var:parallel_1} / {var:parallel_2} / {var:parallel_3} / {var:parallel_4}}
Fuel rod block tiers {gold:1-4} allow heat capacity: {gold:{var:capacity_1} / {var:capacity_2} / {var:capacity_3} / {var:capacity_4}}
Parallel setting controls the number of fuel rods in newly loaded batches
Consumes shortest remaining lifetime first
Exceeding heat capacity does not immediately stop the reactor; after {gold:{var:meltdown_seconds} continuous seconds}, core water evaporates and fuel rod blocks melt into lava
{gray:{hr}}
{aqua:Optional breeding}: paired breeder input/output buses enable breeder rod material conversion
Separate breeder pool uses the fuel rod tier limit without using fuel parallel, including pending products
Every breeder rod gains {gold:P} progress per cycle; P = pulses x {gold:4}
{gray:{hr}}
{aqua:Connected module expansion}: the main structure provides {gold:1} fuel batch slot; each valid module adds {gold:1} independent slot
Main structure steel / titanium / tungstensteel pipes allow {gold:{var:steel_modules}} / {gold:{var:titanium_modules}} / {gold:{var:tungstensteel_modules}} expansion modules, excluding the main structure
Both the back and right sides of the main structure accept modules simultaneously; complete modules touch wall-to-wall in the same horizontal facing and expand in all four horizontal directions, without connecting pipes or shared blocks
Module fuel rod and pipe block tiers must match the corresponding main structure tiers; mismatched modules are invalid
Lowercase channel extra: {gold:1} main only; {gold:2} main + one back module; {gold:3} two consecutive back modules. Right-click the blueprint in air to configure hints and auto-build examples; actual module connections remain flexible
Count changes while enabled or prepaid require a manual restart
Each batch has its own parallel limit and lifetime and may use different fuel; total power and heat are summed across batches
Pending spent rods retain their slot while other batches keep running; expansion does not increase heat capacity, cooling capacity or breeder pool size
{gray:{hr}}
{aqua:Wireless output upgrade}: right-click with this mod's Wireless Upgrade Chip; retained when dismantled
No dynamos: wireless output to the controller owner's GT network; installed dynamos: wired output
{green:Supports Multi-Amp Dynamos and Laser Dynamos}
