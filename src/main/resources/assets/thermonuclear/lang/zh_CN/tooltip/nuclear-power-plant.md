数值设计尚不完善,如有问题欢迎修改
{gray:{hr}}
核电，轻而易举呀
{aqua:直接发电模式}
消耗燃料棒输出电力
发电（EU/t）= {gold:E × C × P × N × S × M × η}
E：燃料每脉冲基础发电值；M：IC2 核电倍率 × 发电倍率={gold:{var:multiple}}；η：发电效率={gold:{var:efficiency}}
{gray:{hr}}
{aqua:供热模式}
消耗燃料产生热量，输出热流体
发热量 = {gold:C × round[P × (P + 1) / 2 × H] × N × S × M × η × R / {var:cycle_ticks}}
H：燃料基础热值；M：配置供热倍率；η：配置供热效率；R：本周期平均启动倍率
支持 IC2 冷却液、超级冷却液、钾钠合金复合冷却液，输入蒸馏水则产出普通蒸汽
{gray:{hr}}
{aqua:特性与支持}
C：单根燃料棒内燃料单元数；N：当前批次燃料棒数量；S：配置燃耗（周期/秒）
每单元脉冲 {gold:P = 1 + floor(C / 2) + 反射脉冲}；批次超过 {gold:4} 根时追加配置反射脉冲，否则为 {gold:0}
燃料棒方块 {gold:1—4} 级每批最大并行：{gold:{var:parallel_1} / {var:parallel_2} / {var:parallel_3} / {var:parallel_4}}
燃料棒方块 {gold:1—4} 级热量上限：{gold:{var:capacity_1} / {var:capacity_2} / {var:capacity_3} / {var:capacity_4}}
在并行设置控制里可以设置每批输入的燃料棒数量,在输入批次时会优先消耗剩余寿命较短的燃料棒
过热不会立即停机；连续过热达到 {gold:{var:meltdown_seconds} 秒} 后蒸干堆芯结构水，并将燃料棒方块融为岩浆
{gray:{hr}}
{aqua:可选增殖功能}：安装增殖输入／输出总线，可以转换大量元素
增殖燃料棒存储与普通燃料棒独立，两者存放上限一致，受燃料棒方块等级影响
每根增殖棒每周期可获得P点进度。P=运行中燃料棒的总脉冲 × {gold:4}
{gray:{hr}}
{aqua:连通模块扩展}：主结构提供 {gold:1} 个燃料批次，每个有效模块增加 {gold:1} 个独立批次
主结构钢／钛／钨钢管道分别允许 {gold:{var:steel_modules}}／{gold:{var:titanium_modules}}／{gold:{var:tungstensteel_modules}} 片扩展模块（不含主结构）
主结构背面与右侧可同时安装模块；完整模块保持同一水平朝向、贴墙连接，可向前后左右继续扩展
模块内的燃料棒和管道方块等级必须同主结构一致，否则不作为有效模块
通道 extra 显示模块连接示例，实际搭建时可以贴着墙壁自由拼接
独立批次用于提供多线程运行燃料棒，可同时运行不同燃料棒，并且合并发电和热量
{gray:{hr}}
{aqua:无线升级}：安装无线升级芯片后解锁
无动力仓时自动输出至无线电网
{green:支持 多A动力仓 激光仓}
