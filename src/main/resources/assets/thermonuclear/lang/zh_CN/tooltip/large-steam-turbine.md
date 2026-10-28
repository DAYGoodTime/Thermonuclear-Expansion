数值设计尚不完善,如有问题欢迎修改
{gray:{hr}}
{aqua:蒸汽发电}
启动需要预热，发电量只取决于输入，在GUI内可以设置每周期蒸汽输入速率
不同蒸汽有不同的发电系数 K（单位 EU/L）
普通蒸汽 {gold:{var:steam_energy}}；过热蒸汽 {gold:{var:superheated_energy}}
超临界蒸汽 {gold:{var:supercritical_energy}}；超超临界蒸汽 {gold:{var:ultra_supercritical_energy}}
低压轴支持普通／过热／超临界蒸汽；高压轴额外支持超超临界蒸汽,并且给K值增加50%
最终发电量 V = 输入 x K x {gold:{var:multiple}}
每周期返还全量的蒸馏水，比例为 {gold:{var:steam_per_water} L} : {gold:1 L}
{gray:{hr}}
{aqua:无线输出升级}：安装无线升级芯片后解锁
无动力仓时自动无线输出至无线电网
{green:支持 多A动力仓 激光仓}
