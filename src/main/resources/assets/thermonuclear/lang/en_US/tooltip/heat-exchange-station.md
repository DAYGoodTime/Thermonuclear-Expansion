Development prototype; values subject to change
{gray:{hr}}
{aqua:Steam production}
Steam (L) = hot coolant (L) x K; K: yield of the coolant's unique conversion path
IC2 hot coolant: superheated steam only, K = {gold:{var:ic2_coefficient}}
Hot super coolant: supercritical steam only, K = {gold:{var:super_coefficient}}
Hot NaK composite coolant: ultra-supercritical steam only, K = {gold:{var:nak_coefficient}}
{gray:{hr}}
{aqua:Exchange mechanics and controls}
Returns matching cold coolant at {gold:1:1}; yields from different hot fluids are not pooled
Consumes {gold:1 L} distilled water per {gold:{var:steam_per_water} L} steam; prepaid water credit and fractional steam carry across batches
Each cycle processes one matching hot fluid; throughput is limited by hot fluid, distilled water and both output paths
Input hot coolant automatically selects its unique steam output; no steam mode switching required
