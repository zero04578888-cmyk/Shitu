package com.shitu.app.model

/**
 * 第十六阶段：真实中国城市数据仓库（数据层）。
 *
 * UI 与游戏逻辑都只通过本仓库读取城市数据，更新城市时不需要改动画与逻辑。
 * 城市名称、行政层级、是否省会 / 副省级 / 计划单列 / 直辖市均以现实为准；
 * 经济、人口、财政、产业、文化、科教、国际化等为归一化到 0～100 的游戏数值。
 *
 * 每行一个城市，字段用「|」分隔：
 * id|name|province|level|powerWeight|economic|politics|culture|education|technology|
 * transportation|international|strategic|gdp|gdpGrowth|population|fiscal|industrial|
 * service|foreignTrade|investment|history|tourism|trait|potential|challenge|
 * industries|tags|summary
 */
object CityDataRepository {

    private const val SEP = '|'

    private val TABLE: String = """
beijing|北京|北京市|M|100|92|100|96|98|96|95|94|100|92|55|78|96|74|96|84|92|98|88|BAL|78|100|科技创新,总部经济,金融服务,文化创意,人工智能,生物医药,集成电路|capital,politics,technology,finance,education,culture,international|全国政治、文化、国际交往与科技创新中心。
shanghai|上海|上海市|M|100|100|96|92|92|94|98|100|98|100|55|84|100|88|100|100|98|88|90|ECON|84|98|金融,航运,汽车,集成电路,生物医药,人工智能,高端制造,国际贸易|finance,shipping,manufacturing,technology,trade,international|国际经济、金融、贸易、航运与科技创新中心。
tianjin|天津|天津市|M|88|78|86|78|80|78|88|78|86|74|48|72|80|84|74|78|78|84|76|IND|76|78|石油化工,装备制造,汽车,港口物流,航空航天,生物医药|manufacturing,port,industry,technology|北方重要港口城市与先进制造基地。
chongqing|重庆|重庆市|M|92|86|90|82|82|82|88|78|92|86|62|90|84|88|78|76|86|86|86|IND|88|84|汽车,电子信息,装备制造,化工,物流,消费品|manufacturing,logistics,technology,industry|西部唯一直辖市，长江上游经济中心。
guangzhou|广州|广东省|SP|94|92|88|88|86|86|94|88|90|92|55|88|90|82|92|94|88|90|84|ECON|84|94|汽车,商贸,医药,制造业,外贸,会展|trade,manufacturing,commerce,logistics|国家中心城市，千年商都，综合门户枢纽。
shenzhen|深圳|广东省|SPP|96|96|84|80|78|100|90|92|96|96|66|88|94|90|88|96|94|60|74|ECON|94|96|科技,电子信息,新能源,通信,互联网,金融,先进制造|technology,innovation,finance,manufacturing,trade|经济特区，科技创新与对外开放高地。
nanjing|南京|江苏省|SP|88|82|84|90|92|86|88|76|84|82|55|80|84|78|84|78|82|94|86|BAL|80|86|电子信息,软件,汽车,化工,科教,文旅|education,technology,culture,manufacturing|东部重要中心城市，科教资源密集。
hangzhou|杭州|浙江省|SP|92|90|82|88|84|92|86|82|86|90|68|84|90|78|94|84|90|92|92|ECON|90|92|数字经济,电商,平台经济,人工智能,民营经济,文旅,金融|technology,digital,ecommerce,internet,finance,culture|数字经济第一城，民营经济活跃。
jinan|济南|山东省|SP|82|74|80|84|82|78|84|70|78|74|55|76|76|76|76|70|74|90|82|BAL|76|74|装备制造,信息技术,生物医药,钢铁,文旅|manufacturing,technology,culture|山东省会，泉城，环渤海地区南翼中心。
qingdao|青岛|山东省|SPP|88|84|78|80|80|82|90|84|86|84|58|78|86|86|82|88|84|82|90|ECON|82|82|家电,海洋经济,港口物流,轨道交通,高端制造,化工|manufacturing,port,trade,ocean,technology|计划单列市与沿海重要港口城市。
wuhan|武汉|湖北省|SP|90|82|84|82|92|86|94|72|88|84|58|86|82|84|80|74|84|88|78|BAL|86|84|光电子,汽车,生物医药,钢铁,科教,物流|technology,manufacturing,education,logistics|中部地区中心城市，九省通衢。
chengdu|成都|四川省|SP|90|86|84|92|86|86|88|80|88|86|66|88|86|80|88|78|88|92|94|BAL|88|84|电子信息,航空航天,生物医药,文创,消费,软件|culture,technology,consumption,manufacturing,tourism|西部地区重要中心城市，宜居与消费之城。
xian|西安|陕西省|SP|86|76|80|94|92|88|86|74|88|76|60|82|76|80|76|70|80|100|96|BAL|84|78|航空航天,国防科技,电子信息,装备制造,高校科研,文旅|aerospace,defense,education,technology,culture,history|世界历史文化名城，国防与科教重镇。
shenyang|沈阳|辽宁省|SP|82|70|76|76|80|78|82|68|80|70|45|76|72|84|70|68|70|86|74|IND|78|70|装备制造,汽车,航空航天,机器人,化工|manufacturing,industry,technology|东北中心城市与先进装备制造基地。
dalian|大连|辽宁省|SPP|82|74|72|76|78|78|86|78|82|72|48|72|78|80|76|82|76|82|86|IND|76|74|船舶,石化,装备制造,软件,港口物流,旅游|manufacturing,port,industry,trade|计划单列市，东北亚航运与软件名城。
changchun|长春|吉林省|SP|78|66|72|72|78|76|78|62|78|66|48|74|68|84|66|62|68|78|70|IND|76|66|汽车,轨道客车,农产品加工,生物医药,光电|manufacturing,industry,agriculture|中国汽车工业与轨道客车基地。
haerbin|哈尔滨|黑龙江省|SP|78|64|72|76|78|74|78|66|76|64|45|74|66|76|68|68|66|82|82|IND|74|64|装备制造,食品加工,医药,航空航天,冰雪文旅|manufacturing,industry,tourism,culture|东北北部中心城市，冰雪文化名城。
xiamen|厦门|福建省|SPP|82|76|70|78|78|80|84|84|84|72|60|66|80|74|80|86|78|78|90|TOUR|82|76|电子信息,航运物流,旅游会展,金融服务,生物医药|trade,port,tourism,technology,international|计划单列市与对台交流门户。
ningbo|宁波|浙江省|SPP|86|84|74|76|76|80|88|82|84|84|60|76|88|88|78|92|86|80|78|ECON|82|80|港口物流,石化,汽车零部件,家电,外贸,高端制造|port,trade,manufacturing,industry|计划单列市与全球货物吞吐大港。
shijiazhuang|石家庄|河北省|CAP|72|62|68|66|68|64|78|56|68|62|50|78|64|72|66|58|64|76|68|IND|74|62|医药,纺织,化工,装备制造,食品|manufacturing,industry,medicine|河北省会与华北重要交通枢纽。
taiyuan|太原|山西省|CAP|72|60|68|68|68|66|76|54|68|60|48|72|66|78|62|54|62|80|72|RES|72|60|煤炭,不锈钢,装备制造,化工,新材料|resources,manufacturing,industry|山西省会，能源与新材料基地。
hefei|合肥|安徽省|CAP|80|74|72|74|82|84|82|66|78|76|72|78|78|80|74|72|84|74|70|BAL|86|72|家电,电子信息,新能源汽车,人工智能,量子科技|technology,manufacturing,innovation,education|综合性国家科学中心，新兴产业增长快。
fuzhou|福州|福建省|CAP|76|68|68|74|72|70|78|72|74|68|58|72|72|70|72|76|72|84|78|BAL|76|66|电子信息,纺织化纤,轻工食品,冶金,海洋经济|manufacturing,trade,culture|福建省会，滨江滨海生态城市。
nanchang|南昌|江西省|CAP|74|66|68|70|72|70|78|60|70|66|58|70|68|74|66|62|70|80|74|BAL|78|64|电子信息,汽车,航空制造,医药,食品|manufacturing,technology,industry|江西省会与航空工业基地。
zhengzhou|郑州|河南省|CAP|80|74|72|74|76|74|92|66|82|78|60|86|76|78|76|72|78|86|76|BAL|82|74|电子信息,汽车,装备制造,物流,食品加工|logistics,manufacturing,technology,transportation|国家中心城市与全国铁路枢纽。
changsha|长沙|湖南省|CAP|80|74|70|82|78|78|82|64|74|76|62|78|76|80|76|66|78|86|86|BAL|82|70|工程机械,电子信息,文化传媒,食品,新材料|manufacturing,culture,technology,media|湖南省会，工程机械与文创之都。
nanning|南宁|广西壮族自治区|CAP|72|60|66|68|66|62|76|66|72|58|52|72|62|64|66|68|64|72|74|BAL|76|58|电子信息,铝加工,食品,生物医药,物流|manufacturing,trade,logistics|广西首府，面向东盟开放门户。
haikou|海口|海南省|CAP|68|58|62|64|64|60|74|70|72|54|58|58|62|50|78|66|66|70|88|TOUR|80|56|旅游,免税消费,生物医药,互联网,热带农业|tourism,consumption,agriculture,international|海南省会，热带滨海旅游城市。
guiyang|贵阳|贵州省|CAP|72|62|66|68|66|70|76|58|72|62|66|70|66|70|68|58|72|72|78|BAL|82|60|大数据,电子信息,装备制造,磷化工,旅游|digital,technology,manufacturing,tourism|贵州省会，大数据产业基地。
kunming|昆明|云南省|CAP|74|64|68|74|70|68|80|72|78|64|56|74|68|68|74|72|70|84|92|TOUR|80|62|旅游,生物医药,有色金属,花卉,物流,现代农业|tourism,agriculture,biology,trade|云南省会，面向南亚东南亚门户。
lanzhou|兰州|甘肃省|CAP|68|54|62|66|68|64|78|56|74|52|50|64|60|70|62|54|60|76|72|BAL|76|54|石化,装备制造,生物制品,有色冶金,物流|manufacturing,industry,logistics|甘肃省会，西北交通与工业重镇。
xining|西宁|青海省|CAP|62|48|58|62|62|58|68|50|68|44|52|56|54|62|60|44|56|70|76|LATE|78|46|新能源,有色金属,化工,高原农牧,旅游|resources,energy,tourism,agriculture|青海省会，高原生态与清洁能源城市。
yinchuan|银川|宁夏回族自治区|CAP|62|48|58|60|60|58|68|48|66|44|52|54|54|62|58|46|56|70|72|LATE|78|46|葡萄酒,煤化工,新材料,装备制造,现代农业|resources,agriculture,manufacturing|宁夏首府，塞上湖城。
urumqi|乌鲁木齐|新疆维吾尔自治区|CAP|72|60|66|68|68|64|78|74|82|60|54|66|64|68|68|74|68|76|84|BAL|80|60|石化,煤化工,装备制造,物流,旅游,现代农业|resources,logistics,trade,tourism|新疆首府，向西开放重要枢纽。
huhehaote|呼和浩特|内蒙古自治区|CAP|66|54|62|64|64|60|72|52|70|54|50|60|58|66|62|50|60|72|74|BAL|76|52|乳业,能源,化工,装备制造,大数据|agriculture,energy,manufacturing,digital|内蒙古首府，乳业与能源基地。
lasa|拉萨|西藏自治区|CAP|60|40|58|78|56|50|62|56|74|38|60|44|56|42|66|38|58|92|88|LATE|80|40|旅游,净土健康,清洁能源,文化,农牧|tourism,culture,energy,agriculture|西藏首府，高原历史文化名城。
suzhou|苏州|江苏省|ORD|86|90|74|82|78|84|86|84|84|92|58|80|92|92|80|94|92|92|86|ECON|84|88|制造业,电子信息,高端装备,生物医药,纳米技术,外贸|manufacturing,technology,trade,industry|最强地级市之一，外向型制造业高地。
wuxi|无锡|江苏省|ORD|80|78|68|76|74|78|82|76|76|78|56|72|82|84|74|84|80|84|78|ECON|78|76|集成电路,物联网,高端装备,纺织,新能源|manufacturing,technology,industry,trade|物联网与集成电路产业名城。
xuzhou|徐州|江苏省|ORD|72|64|64|70|70|66|82|54|72|68|54|78|68|78|66|56|68|82|74|IND|74|64|工程机械,能源,钢铁,食品,物流|manufacturing,industry,logistics|淮海经济区中心城市。
changzhou|常州|江苏省|ORD|72|68|62|68|70|72|78|68|70|70|56|68|74|82|68|78|74|78|70|IND|76|66|新能源,智能制造,轨道交通,纺织,化工|manufacturing,technology,industry|新能源与智能制造基地。
nantong|南通|江苏省|ORD|72|68|62|68|70|70|80|70|72|70|58|74|74|80|66|78|74|80|70|BAL|76|66|船舶海工,纺织,电子信息,家纺,建筑|manufacturing,industry,trade|江海门户，船舶海工基地。
wenzhou|温州|浙江省|ORD|76|74|64|70|68|70|76|74|72|72|58|76|78|78|74|80|76|76|74|ECON|78|76|民营经济,轻工,电气,鞋服,外贸,金融|commerce,manufacturing,trade,finance|民营经济发源地，温商网络遍布全球。
shaoxing|绍兴|浙江省|ORD|70|66|60|78|68|68|76|64|68|68|54|68|72|78|68|74|72|88|80|BAL|74|64|纺织,印染,黄酒,电子信息,生物医药|manufacturing,culture,industry|历史文化名城，纺织与黄酒之乡。
foshan|佛山|广东省|ORD|78|76|66|76|70|74|84|70|74|80|54|80|82|88|72|80|78|84|76|IND|78|74|家电,陶瓷,装备制造,纺织,家具|manufacturing,industry,technology|制造业大市，家电与陶瓷之都。
dongguan|东莞|广东省|ORD|76|74|62|66|66|74|82|72|72|78|52|84|80|88|68|86|76|66|66|IND|76|72|电子信息,智能终端,装备制造,纺织服装,外贸|manufacturing,technology,trade,industry|世界工厂，智能终端制造基地。
zhuhai|珠海|广东省|ORD|70|64|60|68|70|72|76|72|74|62|58|58|72|70|74|76|72|72|86|TOUR|80|62|电子信息,家用电器,生物医药,旅游,海洋经济|technology,tourism,manufacturing,trade|经济特区，宜居滨海城市。
huizhou|惠州|广东省|ORD|68|62|58|64|64|68|76|60|66|64|58|72|68|78|66|70|68|68|76|BAL|76|60|电子信息,石化,新能源,旅游,农业|manufacturing,technology,tourism|电子信息与石化产业并重。
quanzhou|泉州|福建省|ORD|72|68|60|78|66|68|78|70|70|70|56|78|74|80|68|78|72|88|80|BAL|76|64|纺织鞋服,建材,石化,食品,外贸|manufacturing,trade,culture,industry|海上丝绸之路起点，民营制造大市。
tangshan|唐山|河北省|ORD|72|64|62|64|66|66|80|56|72|68|46|76|72|86|60|58|66|74|68|IND|72|62|钢铁,装备制造,化工,陶瓷,港口物流|manufacturing,industry,port|北方工业重镇，钢铁与装备基地。
luoyang|洛阳|河南省|ORD|68|56|60|84|68|66|76|54|68|56|52|74|62|72|62|54|62|96|88|TOUR|76|56|装备制造,新材料,石化,旅游,牡丹产业|culture,history,tourism,manufacturing|十三朝古都，先进制造与文旅名城。
nanyang|南阳|河南省|ORD|62|48|56|64|62|58|72|44|64|48|52|82|56|62|58|46|56|82|76|AGRI|74|48|农业,食品加工,装备制造,医药,文旅|agriculture,food,manufacturing,culture|南水北调中线水源地，农业大市。
zhoukou|周口|河南省|ORD|56|40|54|58|58|52|68|38|58|40|50|84|50|54|52|38|48|74|62|AGRI|76|40|农业,食品加工,纺织,医药,劳动力经济|agriculture,food,manufacturing|豫东农业与食品加工大市。
yantai|烟台|山东省|ORD|74|68|64|70|70|70|80|74|74|72|54|72|76|80|70|80|74|78|82|BAL|78|66|化工,黄金,汽车,电子信息,海洋经济,葡萄酒|manufacturing,industry,trade,tourism|环渤海港口城市与制造业基地。
weifang|潍坊|山东省|ORD|68|60|58|70|66|66|76|58|66|64|54|78|66|76|62|64|66|82|76|AGRI|76|60|现代农业,食品加工,动力装备,化工,纺织|agriculture,food,manufacturing|全国农业现代化先行区。
zibo|淄博|山东省|ORD|68|60|58|70|66|68|76|56|66|60|44|68|66|82|60|60|62|84|74|IND|70|58|化工,新材料,装备制造,陶瓷,医药|manufacturing,industry,chemicals|老工业基地，化工与新材料名城。
guilin|桂林|广西壮族自治区|ORD|66|52|56|78|64|60|72|72|68|50|50|66|58|56|76|60|58|84|96|TOUR|78|52|旅游,电子信息,生物医药,食品,橡胶|tourism,culture,technology|山水甲天下，国际旅游名城。
liuzhou|柳州|广西壮族自治区|ORD|66|56|56|64|64|62|76|50|66|58|48|68|62|78|60|54|60|68|70|IND|72|56|汽车,机械,钢铁,化工,食品|manufacturing,industry,automotive|西南工业重镇，汽车与机械基地。
huangshan|黄山|安徽省|ORD|60|46|54|76|58|54|68|62|62|42|54|48|54|46|72|48|52|88|94|TOUR|76|46|旅游,茶叶,绿色食品,文创,康养|tourism,culture,agriculture|徽文化核心区，世界级旅游目的地。
zhangjiajie|张家界|湖南省|ORD|58|44|52|70|56|52|66|66|60|40|58|42|52|42|76|44|52|76|96|TOUR|78|44|旅游,民宿,文创,特色农业|tourism,culture,agriculture|以峰林地貌闻名的国际旅游城市。
sanya|三亚|海南省|ORD|62|52|54|64|58|56|72|78|70|48|62|46|58|38|82|62|64|66|98|TOUR|82|52|旅游,免税消费,康养,海洋经济,会展|tourism,consumption,international,ocean|热带滨海度假与免税消费城市。
datong|大同|山西省|ORD|60|48|54|68|60|56|72|46|62|48|44|62|58|74|58|44|54|86|76|RES|70|48|煤炭,电力,装备制造,文旅,新材料|resources,energy,manufacturing,tourism|国家历史文化名城，能源转型城市。
ordos|鄂尔多斯|内蒙古自治区|ORD|66|58|56|60|62|58|70|48|66|66|50|52|74|78|58|50|62|66|70|RES|74|52|能源,煤化工,羊绒,装备制造,现代农牧|resources,energy,manufacturing|能源资源富集，人均经济水平较高。
yulin|榆林|陕西省|ORD|62|56|54|60|60|58|68|44|66|62|54|58|70|80|54|46|60|72|68|RES|74|50|煤炭,油气,化工,新材料,现代农业|resources,energy,chemicals|陕北能源化工基地。
daqing|大庆|黑龙江省|ORD|62|52|52|58|62|62|70|46|64|54|42|56|66|80|54|46|54|66|64|RES|68|46|石油,石化,装备制造,新能源,农牧|resources,energy,chemicals|石油工业名城，资源型城市转型样本。
baotou|包头|内蒙古自治区|ORD|62|54|54|60|62|60|72|48|64|56|46|58|62|80|56|50|58|68|66|IND|72|50|钢铁,稀土,装备制造,新能源,化工|manufacturing,resources,industry|稀土之都，钢铁与装备制造基地。
mianyang|绵阳|四川省|ORD|62|54|54|64|66|66|70|48|68|54|56|62|60|72|60|50|62|72|72|BAL|78|52|电子信息,国防科技,装备制造,食品,科技城|technology,defense,manufacturing|中国科技城，国防科研重镇。
zunyi|遵义|贵州省|ORD|62|54|56|66|60|58|72|46|64|56|60|70|62|70|58|46|62|84|80|BAL|78|52|白酒,装备制造,能源,化工,茶叶|manufacturing,food,culture,tourism|历史名城与白酒产业基地。
dali|大理|云南省|ORD|58|44|52|72|58|52|68|58|60|42|56|48|52|44|74|46|54|82|92|TOUR|78|44|旅游,高原农业,生物医药,文化创意|tourism,culture,agriculture,biology|苍山洱海，白族文化名城。
kashgar|喀什|新疆维吾尔自治区|ORD|58|40|56|68|56|50|66|62|78|38|62|56|54|48|62|66|58|82|82|LATE|84|42|商贸物流,纺织,现代农业,文旅,加工制造|trade,logistics,agriculture,tourism|丝绸之路重镇，向西开放前沿。
""".trimIndent()

    /** 全部城市（按数据表中的顺序）。 */
    val cities: List<CityData> = TABLE.lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .map { parse(it) }

    private val byId: Map<String, CityData> = cities.associateBy { it.id }
    private val byName: Map<String, CityData> = cities.associateBy { it.name }

    /** 旧存档兜底城市：普通地级市，避免旧档直接跳到副省级或直辖市。 */
    val default: CityData = find("luoyang") ?: cities.first()

    val municipalities: List<CityData> = cities.filter { it.isMunicipality }
    val subProvincialCities: List<CityData> = cities.filter { it.isSubProvincial }
    val separatelyPlannedCities: List<CityData> = cities.filter { it.isSeparatelyPlanned }
    val provincialCapitals: List<CityData> = cities.filter { it.isProvincialCapital }
    val ordinaryCities: List<CityData> = cities.filter { it.level == CityLevel.ORDINARY }

    /** 省级行政区（由城市聚合）。 */
    val provinces: List<ProvinceData> = cities
        .groupBy { it.province }
        .map { (province, list) ->
            ProvinceData(
                name = province,
                region = regionOfProvince(province),
                cityIds = list.map { it.id },
                capitalCityId = list.firstOrNull { it.isProvincialCapital }?.id
                    ?: list.first().id
            )
        }
        .sortedBy { it.name }

    fun find(id: String?): CityData? = id?.let { byId[it] }

    fun findByName(name: String?): CityData? = name?.let { byName[it] }

    fun findOrDefault(id: String?): CityData = find(id) ?: default

    /** 按种子稳定地取一个城市，用于不指定城市时随机分配。 */
    fun randomSeeded(seed: Long): CityData {
        val size = cities.size
        return cities[((seed % size) + size).toInt() % size]
    }

    fun withTrait(trait: RegionTrait): List<CityData> = cities.filter { it.trait == trait }

    fun byLevel(level: CityLevel): List<CityData> = cities.filter { it.level == level }

    fun byProvince(province: String): List<CityData> = cities.filter { it.province == province }

    /** 简单搜索：按城市名或省份匹配。 */
    fun search(query: String): List<CityData> {
        val q = query.trim()
        if (q.isEmpty()) return cities
        return cities.filter { it.name.contains(q) || it.province.contains(q) || it.industries.any { tag -> tag.contains(q) } }
    }

    /**
     * 生成一座城市内部各领导职务的权力档案。
     * 同一城市里书记、市长、副书记、常委、常务副市长、委办局负责人的
     * 权力构成必须不同，这是「职位等级计算规则」的一部分。
     */
    fun positionSpecs(city: CityData): List<CityPositionSpec> =
        CityPositionRole.entries.map { role -> specFor(city, role) }

    fun specFor(city: CityData, role: CityPositionRole): CityPositionSpec {
        val rankIndex = rankIndexOf(city.level, role)
        fun scaled(base: Int): Int = (base * 70 + base * city.powerWeight * 30 / 100) / 100
        val (political, economic, organizational, public, promotion) = when (role) {
            CityPositionRole.PARTY_SECRETARY -> listOf(100, 78, 96, 86, 30)
            CityPositionRole.MAYOR -> listOf(82, 100, 88, 84, 26)
            CityPositionRole.DEPUTY_SECRETARY -> listOf(90, 76, 96, 72, 24)
            CityPositionRole.STANDING_COMMITTEE -> listOf(92, 72, 82, 68, 20)
            CityPositionRole.EXECUTIVE_VICE_MAYOR -> listOf(76, 96, 84, 74, 22)
            CityPositionRole.BUREAU_HEAD -> listOf(62, 74, 68, 76, 14)
        }
        return CityPositionSpec(
            cityId = city.id,
            role = role,
            title = titleFor(city, role),
            rankIndex = rankIndex,
            powerLevel = positionAt(rankIndex).powerLevel,
            politicalInfluence = scaled(political).coerceIn(0, 100),
            economicInfluence = scaled(economic).coerceIn(0, 100),
            organizationalInfluence = scaled(organizational).coerceIn(0, 100),
            publicInfluence = scaled(public).coerceIn(0, 100),
            promotionPotential = scaled(promotion).coerceIn(0, 100)
        )
    }

    /** 城市内某个职务的正式名称，例如「深圳市市长」。 */
    fun titleFor(city: CityData, role: CityPositionRole): String = when (role) {
        CityPositionRole.PARTY_SECRETARY -> "${city.name}市委书记"
        CityPositionRole.MAYOR -> "${city.name}市长"
        CityPositionRole.DEPUTY_SECRETARY -> "${city.name}市委副书记"
        CityPositionRole.STANDING_COMMITTEE -> "${city.name}市委常委"
        CityPositionRole.EXECUTIVE_VICE_MAYOR -> "${city.name}常务副市长"
        CityPositionRole.BUREAU_HEAD -> "${city.name}委办局主要负责人"
    }

    /** 某层级下党政主官对应的职级序号。 */
    fun rankIndexOf(level: CityLevel, role: CityPositionRole): Int = when (role) {
        CityPositionRole.PARTY_SECRETARY -> level.secretaryRankIndex
        CityPositionRole.MAYOR -> level.mayorRankIndex
        CityPositionRole.DEPUTY_SECRETARY -> level.mayorRankIndex
        CityPositionRole.STANDING_COMMITTEE -> level.mayorRankIndex - 1
        CityPositionRole.EXECUTIVE_VICE_MAYOR -> level.mayorRankIndex - 1
        CityPositionRole.BUREAU_HEAD -> level.mayorRankIndex - 2
    }.coerceIn(0, POSITION_LADDER.size - 1)

    /** 城市所在大区。 */
    fun regionOfProvince(province: String): String = when (province) {
        "北京市", "天津市", "河北省", "山西省", "内蒙古自治区" -> "华北"
        "辽宁省", "吉林省", "黑龙江省" -> "东北"
        "上海市", "江苏省", "浙江省", "安徽省", "福建省", "江西省", "山东省" -> "华东"
        "河南省", "湖北省", "湖南省" -> "华中"
        "广东省", "广西壮族自治区", "海南省" -> "华南"
        "重庆市", "四川省", "贵州省", "云南省", "西藏自治区" -> "西南"
        else -> "西北"
    }

    private fun parse(line: String): CityData {
        val parts = line.split(SEP)
        require(parts.size >= 29) { "城市数据字段不足：$line" }
        return CityData(
            id = parts[0].trim(),
            name = parts[1].trim(),
            province = parts[2].trim(),
            level = CityLevel.byCode(parts[3]),
            region = regionOfProvince(parts[2].trim()),
            powerWeight = parts[4].trim().toInt(),
            economic = parts[5].trim().toInt(),
            politics = parts[6].trim().toInt(),
            culture = parts[7].trim().toInt(),
            education = parts[8].trim().toInt(),
            technology = parts[9].trim().toInt(),
            transportation = parts[10].trim().toInt(),
            internationalInfluence = parts[11].trim().toInt(),
            strategicImportance = parts[12].trim().toInt(),
            gdp = parts[13].trim().toInt(),
            gdpGrowth = parts[14].trim().toInt(),
            population = parts[15].trim().toInt(),
            fiscalStrength = parts[16].trim().toInt(),
            industrialStrength = parts[17].trim().toInt(),
            serviceIndustryStrength = parts[18].trim().toInt(),
            foreignTrade = parts[19].trim().toInt(),
            investmentStrength = parts[20].trim().toInt(),
            historyLevel = parts[21].trim().toInt(),
            tourismLevel = parts[22].trim().toInt(),
            trait = traitOf(parts[23].trim()),
            potential = parts[24].trim().toInt(),
            challenge = parts[25].trim().toInt(),
            industries = parts[26].split(',').map { it.trim() }.filter { it.isNotEmpty() },
            eventTags = parts[27].split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet(),
            summary = parts[28].trim()
        )
    }

    private fun traitOf(code: String): RegionTrait = when (code.uppercase()) {
        "ECON" -> RegionTrait.ECONOMIC_HUB
        "IND" -> RegionTrait.INDUSTRIAL
        "AGRI" -> RegionTrait.AGRICULTURAL
        "TOUR" -> RegionTrait.TOURISM
        "RES" -> RegionTrait.RESOURCE
        "LATE" -> RegionTrait.UNDERDEVELOPED
        else -> RegionTrait.BALANCED
    }
}