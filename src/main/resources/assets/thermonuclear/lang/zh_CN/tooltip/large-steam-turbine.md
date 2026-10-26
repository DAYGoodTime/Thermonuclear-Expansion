数值设计尚不完善,如有问题欢迎修改
{gray:{hr}}
{aqua:蒸汽发电}
发电（EU/t）= Q × E × R
Q：实际耗汽量（L/t）；R：本 tick 启动倍率（{gold:0—1}），无额外配置发电倍率
E（EU/L）：普通蒸汽 {gold:{var:steam_energy}}；过热蒸汽 {gold:{var:superheated_energy}}
E（EU/L）：超临界蒸汽 {gold:{var:supercritical_energy}}；超超临界蒸汽 {gold:{var:ultra_supercritical_energy}}
低压轴支持普通／过热／超临界蒸汽；高压轴额外支持超超临界蒸汽
{gray:{hr}}
{aqua:发电机制与操作}
预热时间: {gold:{var:startup_ticks} tick}，停机 {gold:{var:decay_ticks} tick} 后涡轮效率归0
每 {gold:{var:steam_per_water} L} 蒸汽回收 {gold:1 L} 蒸馏水，不足整升的余量跨批保留；输出堵塞时停机
GUI 可设置每 {gold:{var:cycle_ticks} tick} 周期耗汽上限，下周期生效；设为 {gold:0} 时停止耗汽
{gray:{hr}}
{aqua:无线输出升级}：使用本模组无线升级芯片右键主机解锁，拆机保留升级
无动力仓时自动无线输出至无线电网；安装动力仓则使用有线输出
{green:支持 多A动力仓 激光仓}
