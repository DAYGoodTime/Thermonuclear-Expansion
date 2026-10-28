Development prototype; values subject to change
{gray:{hr}}
Nuclear power made easy
{aqua:Direct generation mode}
Consumes fuel rods to output power
Generation (EU/t) = {gold:E x C x P x N x S x M x eta}
E: base fuel energy per pulse; M: IC2 nuclear multiplier x direct multiplier = {gold:{var:multiple}}; eta: direct efficiency = {gold:{var:efficiency}}
{gray:{hr}}
{aqua:Heat supply mode}
Consumes fuel to generate heat and outputs hot fluid
Heat generation = {gold:C x round[P x (P + 1) / 2 x H] x N x S x M x eta x R / {var:cycle_ticks}}
H: base fuel heat; M: configured heat multiplier; eta: configured heat efficiency; R: average startup multiplier this cycle
Supports IC2 coolant, super coolant and NaK composite coolant; distilled water input produces ordinary steam
{gray:{hr}}
{aqua:Features and support}
C: cells per fuel rod; N: rods in the current batch; S: configured fuel cycles per second
Pulses per cell: {gold:P = 1 + floor(C / 2) + reflector pulses}; configured reflector pulses apply above {gold:4} rods per batch, otherwise {gold:0}
Fuel rod block tiers {gold:1-4} allow max parallel per batch: {gold:{var:parallel_1} / {var:parallel_2} / {var:parallel_3} / {var:parallel_4}}
Fuel rod block tiers {gold:1-4} allow heat capacity: {gold:{var:capacity_1} / {var:capacity_2} / {var:capacity_3} / {var:capacity_4}}
The parallel setting controls the number of fuel rods loaded per batch; loading a batch consumes the shortest remaining lifetime first
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
Independent batches let fuel rods run in parallel: different rods run simultaneously, and their power and heat are combined
{gray:{hr}}
{aqua:Wireless upgrade}: unlocked by installing the Wireless Upgrade Chip
No dynamos: wireless output to the controller owner's GT network automatically
{green:Supports Multi-Amp Dynamos and Laser Dynamos}
