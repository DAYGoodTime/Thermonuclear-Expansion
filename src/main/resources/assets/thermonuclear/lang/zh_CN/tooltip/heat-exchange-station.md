数值设计尚不完善,如有问题欢迎修改
{gray:{hr}}
{aqua:换热产汽}
产汽（L）= 热冷却液（L）× K；K：对应冷却液唯一路径的产汽系数
IC2 热冷却液：仅过热蒸汽，K = {gold:{var:ic2_coefficient}}
热超级冷却液：仅超临界蒸汽，K = {gold:{var:super_coefficient}}
热钾钠合金复合冷却液：仅超超临界蒸汽，K = {gold:{var:nak_coefficient}}
{gray:{hr}}
{aqua:换热机制与操作}
热冷却液与对应冷冷却液按 {gold:1:1} 返还，不混合不同热流体的产汽量
每 {gold:{var:steam_per_water} L} 蒸汽消耗 {gold:1 L} 蒸馏水，预付水耗额度与不足整升的产汽量跨批保留
每周期仅处理一种匹配热流体，实际吞吐受热流体、蒸馏水与两路输出空间限制
根据输入热冷却液自动选择唯一蒸汽产物，无需切换蒸汽模式
