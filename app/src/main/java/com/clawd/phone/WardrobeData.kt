package com.clawd.phone

// СГЕНЕРИРОВАНО tools/gen_wardrobe.py из clawd_wardrobe.py (Clawd для ПК) — правь там, потом перегенерируй.

object WardrobeData {
    val SLOTS = listOf(Triple("head", "Голова", "🎩"), Triple("face", "Лицо", "🕶"), Triple("neck", "Шея", "🎀"), Triple("hand", "В лапе", "🗡"), Triple("back", "Спина", "🦋"), Triple("pattern", "Узор", "🦓"), Triple("aura", "Аура", "✨"))

    private fun head() = listOf(
        WItem("bow", "head", "Бантик", 1, rows = listOf("m..m", "mxxm"), pal = mapOf('m' to 0xFFFF6FA8.toInt(), 'x' to 0xFFD94C88.toInt()), dx = 3),
        WItem("party", "head", "Колпак", 2, rows = listOf(".ww.", ".yy.", "bbbb"), pal = mapOf('w' to 0xFFFFFFFF.toInt(), 'y' to 0xFFFFC94D.toInt(), 'b' to 0xFF6FB8FF.toInt())),
        WItem("crown", "head", "Корона", "pet100", rows = listOf("g.gg.g", "ggrrgg"), pal = mapOf('g' to 0xFFFFC94D.toInt(), 'r' to 0xFFFF5C7A.toInt())),
        WItem("flower", "head", "Цветок", "days7", rows = listOf(".m.", "mxm", ".m."), pal = mapOf('m' to 0xFFFF6FA8.toInt(), 'x' to 0xFFFFC94D.toInt()), dx = 4, dy = 1),
        WItem("halo", "head", "Нимб", "days30", rows = listOf(".mmmmmm.", "x......x", "........"), pal = mapOf('m' to 0xFFFFE08A.toInt(), 'x' to 0xFFFFC94D.toInt())),
        WItem("phones", "head", "Наушники", 3, rows = listOf("..mmmmmmmmmm..", "..............", "xx..........xx", "xx..........xx", "xx..........xx"), pal = mapOf('m' to 0xFF3A3A3A.toInt(), 'x' to 0xFF5E6670.toInt()), dy = 4),
        WItem("tophat", "head", "Цилиндр", null, rows = listOf("..kkkkkk..", "..kkkkkk..", "..kkkkkk..", "..rrrrrr..", "kkkkkkkkkk"), pal = mapOf('k' to 0xFF1F1F24.toInt(), 'r' to 0xFFC94A3F.toInt())),
        WItem("cowboy", "head", "Ковбойская шляпа", null, rows = listOf("....bbbb....", "...bbbbbb...", "b..dddddd..b", ".bbbbbbbbbb."), pal = mapOf('b' to 0xFF8B5A2B.toInt(), 'd' to 0xFF5A3A1A.toInt())),
        WItem("cap", "head", "Кепка", null, rows = listOf("......w.......", "...rrrrrr.....", "..rrrrrrrr....", "..rrrrrrrrvvvv"), pal = mapOf('r' to 0xFF3B7DD8.toInt(), 'w' to 0xFFFFFFFF.toInt(), 'v' to 0xFF2A5AA0.toInt())),
        WItem("beanie", "head", "Шапка-бини", null, rows = listOf(".....pp.....", "...yyyyyy...", "..yyyyyyyy..", "dddddddddddd"), pal = mapOf('y' to 0xFFE5534B.toInt(), 'd' to 0xFFB8322B.toInt(), 'p' to 0xFFFFFFFF.toInt()), dy = 1),
        WItem("chef", "head", "Поварской колпак", 1, rows = listOf("..wwwwww..", ".wwwwwwww.", ".wwwwwwww.", "..wwwwww..", "..gggggg.."), pal = mapOf('w' to 0xFFFFFFFF.toInt(), 'g' to 0xFFDFE3EA.toInt())),
        WItem("viking", "head", "Шлем викинга", 2, rows = listOf("w............w", "ww..........ww", ".wwssssssssww.", "..ssssssssss.."), pal = mapOf('w' to 0xFFF2E6C8.toInt(), 's' to 0xFF8A8F99.toInt()), dy = 1),
        WItem("wizard", "head", "Шляпа волшебника", 3, rows = listOf("........p.....", ".......pp.....", "......ppp.....", ".....ppypp....", "....pppppp....", "...ppyppppp...", "pppppppppppppp"), pal = mapOf('p' to 0xFF6A4FC9.toInt(), 'y' to 0xFFFFD34D.toInt())),
        WItem("witch", "head", "Шляпа ведьмы", 2, rows = listOf("........k.....", ".......kk.....", "......kkk.....", ".....kkkk.....", "....kooook....", "kkkkkkkkkkkkkk"), pal = mapOf('k' to 0xFF241A2E.toInt(), 'o' to 0xFFF08A24.toInt())),
        WItem("pirate", "head", "Треуголка", "throw50", rows = listOf(".....kkkk.....", "...kkkwwkkk...", ".kkkkkkkkkkkk.", "kkyyyyyyyyyykk"), pal = mapOf('k' to 0xFF1F1F24.toInt(), 'w' to 0xFFFFFFFF.toInt(), 'y' to 0xFFE8B84A.toInt())),
        WItem("headband", "head", "Повязка ниндзя", null, rows = listOf("rr..............", ".rrrrrrrrrrrrr.."), pal = mapOf('r' to 0xFFE5534B.toInt()), dy = 1),
        WItem("tiara", "head", "Диадема", 4, rows = listOf("...y....", "..yby...", ".y.y.y.y", "yyyyyyyy"), pal = mapOf('y' to 0xFFFFD34D.toInt(), 'b' to 0xFF6FB8FF.toInt()), dy = 1),
        WItem("antenna", "head", "Антенки", null, rows = listOf("g........g", ".s......s.", "..s....s.."), pal = mapOf('g' to 0xFF8FFF6A.toInt(), 's' to 0xFF3A3A3A.toInt())),
        WItem("catears", "head", "Кошачьи ушки", null, rows = listOf("B..........B", "BB........BB", "BpB......BpB"), pal = mapOf('p' to 0xFFFF9FC0.toInt())),
        WItem("bunny", "head", "Ушки зайки", 1, rows = listOf("..ww....ww..", "..wp....pw..", "..wp....pw..", "..wp....pw..", "..ww....ww.."), pal = mapOf('w' to 0xFFFFFFFF.toInt(), 'p' to 0xFFFFB3CF.toInt())),
        WItem("horns", "head", "Рожки чертёнка", 3, rows = listOf("r..........r", "rr........rr"), pal = mapOf('r' to 0xFFD6332B.toInt())),
        WItem("propeller", "head", "Кепка с пропеллером", 2, rows = listOf("...yyyyyy...", ".....kk.....", "...rrggbb...", "..rrrggbbb.."), pal = mapOf('y' to 0xFFFFD34D.toInt(), 'k' to 0xFF333333.toInt(), 'r' to 0xFFE5534B.toInt(), 'g' to 0xFF4FBF98.toInt(), 'b' to 0xFF4FA8E8.toInt())),
        WItem("mushroom", "head", "Мухомор", 5, rows = listOf("...rrrrrr...", ".rrwrrrrwrr.", "rrrrrrwrrrrr"), pal = mapOf('r' to 0xFFE5534B.toInt(), 'w' to 0xFFFFFFFF.toInt())),
        WItem("frog", "head", "Шапка-лягушка", 4, rows = listOf("..gg....gg..", ".gwkg..gwkg.", "gggggggggggg"), pal = mapOf('g' to 0xFF5CB85C.toInt(), 'w' to 0xFFFFFFFF.toInt(), 'k' to 0xFF111111.toInt()), dy = 1),
        WItem("grad", "head", "Шапочка выпускника", 5, rows = listOf("kkkkkkkkkkkkkk", "...kkkkkkkk.y.", "...kkkkkkkk.y."), pal = mapOf('k' to 0xFF1F1F24.toInt(), 'y' to 0xFFFFD34D.toInt())),
        WItem("wreath", "head", "Венок", "days7", rows = listOf(".p..y..b..p.", "gpggyggbggpg"), pal = mapOf('p' to 0xFFFF6FA8.toInt(), 'y' to 0xFFFFD34D.toInt(), 'b' to 0xFF6FB8FF.toInt(), 'g' to 0xFF4F8A2E.toInt()), dy = 1),
        WItem("crest", "head", "Гребень дракона", 6, rows = listOf(".g...g...g..", "ggg.ggg.ggg."), pal = mapOf('g' to 0xFF6BD16B.toInt())),
        WItem("straw", "head", "Соломенная шляпа", null, rows = listOf(".....yyyyyy.....", ".....rrrrrr.....", "yyyyyyyyyyyyyyyy"), pal = mapOf('y' to 0xFFE8C66A.toInt(), 'r' to 0xFFE5534B.toInt())),
        WItem("sprout", "head", "Росток", null, rows = listOf("gg.gg", ".ggg.", "..s.."), pal = mapOf('g' to 0xFF6BD16B.toInt(), 's' to 0xFF3F8A2E.toInt())),
        WItem("bucket", "head", "Панамка", null, rows = listOf("...bbbbbb...", "..bbbbbbbb..", "bbbbbbbbbbbb"), pal = mapOf('b' to 0xFF5AA0D8.toInt()), dy = 1),
        WItem("crown2", "head", "Королевская корона", 10, rows = listOf("y...y...y", "y.y.y.y.y", "yyyyyyyyy", "yrybyryby", "yyyyyyyyy"), pal = mapOf('y' to 0xFFFFC94D.toInt(), 'r' to 0xFFE5534B.toInt(), 'b' to 0xFF4FA8E8.toInt())),
        WItem("astro", "head", "Шлем космонавта", 8, rows = listOf("...wwwwww...", "..wccccccw..", ".wcc.ccccw..", ".wccccccccw."), pal = mapOf('w' to 0xFFE9ECF2.toInt(), 'c' to 0xFF6FB8FF.toInt()), dy = 1),
        WItem("nightcap", "head", "Ночной колпак", null, rows = listOf("......bbw", "..bbbbb..", "wbbbbbbb."), pal = mapOf('b' to 0xFF4A6FD0.toInt(), 'w' to 0xFFFFFFFF.toInt()), auto = true),
        WItem("umbrella", "head", "Зонтик", null, rows = listOf("...rrrrrrrr...", "rrrrwrrrrwrrrr", ".......k......"), pal = mapOf('r' to 0xFFE5534B.toInt(), 'w' to 0xFFFFFFFF.toInt(), 'k' to 0xFF3A3A3A.toInt()), auto = true),
        WItem("santa", "head", "Шапка Санты", null, rows = listOf("..rrrrrrww", "wwwwwwwwww"), pal = mapOf('r' to 0xFFE53935.toInt(), 'w' to 0xFFFFFFFF.toInt()), auto = true),
    )

    private fun face() = listOf(
        WItem("glasses", "face", "Очки", 3, pal = mapOf('m' to 0xFF1A1A1A.toInt(), 'x' to 0xFF5E6670.toInt()), left = listOf("", "mxmmm", ".mmm."), right = listOf("", "mxmmm", ".mmm."), bridge = listOf(1 to 'm'), edge = listOf(1 to 'm')),
        WItem("eyepatch", "face", "Пиратская повязка", "throw50", pal = mapOf('m' to 0xFF141414.toInt()), left = listOf("mmmmm"), right = listOf("mmmmm", ".mmm.", ".mmm."), bridge = listOf(0 to 'm'), edge = listOf(0 to 'm')),
        WItem("round", "face", "Круглые очки", null, pal = mapOf('m' to 0xFFC9A14A.toInt()), left = listOf(".mmm.", "m...m", "m...m", ".mmm."), right = listOf(".mmm.", "m...m", "m...m", ".mmm."), bridge = listOf(1 to 'm')),
        WItem("hearts", "face", "Очки-сердечки", null, pal = mapOf('m' to 0xFFFF4F8A.toInt()), left = listOf(".m.m.", "mmmmm", ".mmm.", "..m.."), right = listOf(".m.m.", "mmmmm", ".mmm.", "..m.."), bridge = listOf(1 to 'm')),
        WItem("stars", "face", "Очки-звёзды", 2, pal = mapOf('y' to 0xFFFFD34D.toInt()), left = listOf("..y..", "yyyyy", ".yyy.", ".y.y."), right = listOf("..y..", "yyyyy", ".yyy.", ".y.y."), bridge = listOf(1 to 'y')),
        WItem("3d", "face", "3D-очки", 1, pal = mapOf('k' to 0xFFF2F2F2.toInt(), 'r' to 0xFFE5534B.toInt(), 'b' to 0xFF4FA8E8.toInt()), left = listOf("kkkkk", "krrrk", "krrrk"), right = listOf("kkkkk", "kbbbk", "kbbbk"), bridge = listOf(0 to 'k', 1 to 'k'), edge = listOf(0 to 'k')),
        WItem("visor", "face", "Кибер-визор", 6, pal = mapOf('c' to 0xFF39E6FF.toInt(), 'C' to 0xFF1AA6C8.toInt()), left = listOf("", "ccccc", "CCCCC"), right = listOf("", "ccccc", "CCCCC"), bridge = listOf(1 to 'c', 2 to 'C'), edge = listOf(1 to 'c', 2 to 'C')),
        WItem("monocle", "face", "Монокль", 4, pal = mapOf('y' to 0xFFE8B84A.toInt()), right = listOf(".yyy.", "y...y", "y...y", ".yyy.", "....y")),
        WItem("mustache", "face", "Усы", null, pal = mapOf('k' to 0xFF3A2414.toInt()), left = listOf("", "", "", ".kkkk", "kk..."), right = listOf("", "", "", "kkkk.", "...kk"), bridge = listOf(3 to 'k')),
        WItem("blush", "face", "Румянец", null, pal = mapOf('r' to 0xFFFF7AA0.toInt()), left = listOf("", "", "", "rr..."), right = listOf("", "", "", "...rr")),
        WItem("clown", "face", "Нос клоуна", 1, pal = mapOf('r' to 0xFFFF3030.toInt()), left = listOf("", "", "....r", "....r"), right = listOf("", "", "r....", "r...."), bridge = listOf(2 to 'r', 3 to 'r')),
        WItem("sleepmask", "face", "Маска для сна", 3, pal = mapOf('p' to 0xFFFFB3CF.toInt(), 'k' to 0xFFB85A80.toInt()), left = listOf("", "ppppp", "pkkkp", "ppppp"), right = listOf("", "ppppp", "pkkkp", "ppppp"), bridge = listOf(1 to 'p', 2 to 'p', 3 to 'p'), edge = listOf(1 to 'p', 2 to 'p', 3 to 'p')),
        WItem("scar", "face", "Шрам", 5, pal = mapOf('r' to 0xFFB8322B.toInt()), left = listOf(".r...", "..r..", "...r.")),
        WItem("freckles", "face", "Веснушки", null, left = listOf("", "", "", ".D.D."), right = listOf("", "", "", ".D.D.")),
        WItem("lasers", "face", "Лазерные глаза", 9, pal = mapOf('r' to 0xFFFF2A2A.toInt(), 'R' to 0xFFFFB0B0.toInt()), left = listOf("", "..R..", "..r..", ".r...", "r...."), right = listOf("", "..R..", "..r..", "...r.", "....r")),
    )

    private fun neck() = listOf(
        WItem("scarf", "neck", "Шарф", null, rows = listOf("mxmmxmmxmmxm", ".........mx.", ".........m.."), pal = mapOf('m' to 0xFFE5534B.toInt(), 'x' to 0xFFFFC94D.toInt())),
        WItem("bowtie", "neck", "Бабочка", null, rows = listOf("r...r", "rrRrr", "r...r"), pal = mapOf('r' to 0xFFE5534B.toInt(), 'R' to 0xFFA52F28.toInt())),
        WItem("tie", "neck", "Галстук", 1, rows = listOf(".r.", ".r.", "rrr", "rrr", ".r."), pal = mapOf('r' to 0xFF3B5BD8.toInt())),
        WItem("chain", "neck", "Золотая цепь", 6, rows = listOf("y........y", ".yy....yy.", "...yyyy...", "....YY...."), pal = mapOf('y' to 0xFFE8B84A.toInt(), 'Y' to 0xFFFFF1A0.toInt())),
        WItem("medal", "neck", "Медаль", 5, rows = listOf("b...b", ".b.b.", "..y..", ".yyy.", "..y.."), pal = mapOf('b' to 0xFF3B5BD8.toInt(), 'y' to 0xFFFFC94D.toInt())),
        WItem("bell", "neck", "Ошейник с бубенчиком", null, rows = listOf("rrrrrrrrrrrr", ".....yy.....", ".....yy....."), pal = mapOf('r' to 0xFFE5534B.toInt(), 'y' to 0xFFFFD34D.toInt())),
        WItem("bandana", "neck", "Бандана", 2, rows = listOf("rwrrrwrrrwrr", "..rrrrrrrr..", "....rrrr....", ".....rr....."), pal = mapOf('r' to 0xFFD6332B.toInt(), 'w' to 0xFFFFFFFF.toInt())),
        WItem("pearls", "neck", "Жемчуг", 4, rows = listOf("wWwWwWwWwWwW"), pal = mapOf('w' to 0xFFFFFFFF.toInt(), 'W' to 0xFFD9DDE6.toInt())),
        WItem("lei", "neck", "Гавайские бусы", 3, rows = listOf("pypbpypbpypb", "..ppyybbpp.."), pal = mapOf('p' to 0xFFFF6FA8.toInt(), 'y' to 0xFFFFD34D.toInt(), 'b' to 0xFF6FB8FF.toInt())),
        WItem("neckphones", "neck", "Наушники на шее", null, rows = listOf("kk........kk", ".kkkkkkkkkk."), pal = mapOf('k' to 0xFF3A3A3A.toInt())),
        WItem("clover", "neck", "Клевер на удачу", 7, rows = listOf("..gg..", ".gggg.", "..gg..", "...s.."), pal = mapOf('g' to 0xFF4FBF6A.toInt(), 's' to 0xFF2F7A3A.toInt()), dx = 2),
    )

    private fun hand() = listOf(
        WItem("sword", "hand", "Меч", 3, rows = listOf(".s.", ".s.", ".s.", ".s.", "ggg", ".b."), pal = mapOf('s' to 0xFFDFE6F2.toInt(), 'g' to 0xFFE8B84A.toInt(), 'b' to 0xFF7A4A26.toInt()), dx = -1),
        WItem("wand", "hand", "Волшебная палочка", 4, rows = listOf(".y.", "yyy", ".y.", ".k.", ".k.", ".k."), pal = mapOf('y' to 0xFFFFD34D.toInt(), 'k' to 0xFF3A2414.toInt()), dx = -1),
        WItem("balloon", "hand", "Воздушный шарик", null, rows = listOf(".rr.", "rWrr", "rrrr", ".rr.", "..k.", ".k..", "..k.", ".k.."), pal = mapOf('r' to 0xFFFF4F6A.toInt(), 'W' to 0xFFFFB3C0.toInt(), 'k' to 0xFFCFCFCF.toInt()), dx = -1),
        WItem("icecream", "hand", "Мороженое", null, rows = listOf(".p.", "ppp", "yyy", ".y."), pal = mapOf('p' to 0xFFFF9FC0.toInt(), 'y' to 0xFFE0A050.toInt()), dx = -1),
        WItem("fish", "hand", "Рыбка", null, rows = listOf("b.bbb.", ".bbbkb", "b.bbb."), pal = mapOf('b' to 0xFF6FB8FF.toInt(), 'k' to 0xFF111111.toInt())),
        WItem("flag", "hand", "Флажок", 1, rows = listOf("krrr", "krrr", "k...", "k...", "k..."), pal = mapOf('k' to 0xFF3A2414.toInt(), 'r' to 0xFFE5534B.toInt())),
        WItem("lollipop", "hand", "Леденец", null, rows = listOf("ppp", "pwp", "ppp", ".k.", ".k."), pal = mapOf('p' to 0xFFFF6FA8.toInt(), 'w' to 0xFFFFFFFF.toInt(), 'k' to 0xFFDDDDDD.toInt()), dx = -1),
        WItem("coffee", "hand", "Кофе", 2, rows = listOf(".g.g.", "wwww.", "wbbww", "wwww."), pal = mapOf('g' to 0xFFCFCFCF.toInt(), 'w' to 0xFFFFFFFF.toInt(), 'b' to 0xFF6B3E1F.toInt())),
        WItem("phone", "hand", "Телефончик", null, rows = listOf("kk", "cc", "cc", "kk"), pal = mapOf('k' to 0xFF222222.toInt(), 'c' to 0xFF6FB8FF.toInt())),
        WItem("bouquet", "hand", "Букет", 3, rows = listOf("p.y", "rbp", ".g.", ".g."), pal = mapOf('p' to 0xFFFF6FA8.toInt(), 'y' to 0xFFFFD34D.toInt(), 'r' to 0xFFE5534B.toInt(), 'b' to 0xFF6FB8FF.toInt(), 'g' to 0xFF4F8A2E.toInt()), dx = -1),
        WItem("pizza", "hand", "Пицца", 2, rows = listOf("bbbb", "yryy", ".yr.", ".y.."), pal = mapOf('b' to 0xFFC98A3A.toInt(), 'y' to 0xFFFFD34D.toInt(), 'r' to 0xFFE5534B.toInt()), dx = -1),
        WItem("saber", "hand", "Световой меч", 8, rows = listOf("c", "c", "c", "c", "c", "s", "s"), pal = mapOf('c' to 0xFF39E6FF.toInt(), 's' to 0xFF9AA0A8.toInt())),
        WItem("magnifier", "hand", "Лупа", 5, rows = listOf("..kk.", ".k..k", ".k..k", "..kk.", ".k...", "k...."), pal = mapOf('k' to 0xFF3A3A3A.toInt())),
        WItem("trophy", "hand", "Кубок", 12, rows = listOf("y...y", "yyyyy", ".yyy.", "..y..", ".yyy."), pal = mapOf('y' to 0xFFFFC94D.toInt()), dx = -1),
    )

    private fun back() = listOf(
        WItem("angel", "back", "Крылья ангела", "days30", rows = listOf("ww................ww", "www..............www", ".www............www.", "..ww............ww.."), pal = mapOf('w' to 0xFFFFFFFF.toInt()), dy = -1),
        WItem("bat", "back", "Крылья летучей мыши", 4, rows = listOf("k..................k", "kk................kk", "kkk..............kkk", "k.k..............k.k"), pal = mapOf('k' to 0xFF3A2A44.toInt()), dy = -1),
        WItem("butterfly", "back", "Крылья бабочки", 3, rows = listOf("pp................pp", "pyp..............pyp", ".pp..............pp.", "..bb............bb.."), pal = mapOf('p' to 0xFFFF6FA8.toInt(), 'y' to 0xFFFFD34D.toInt(), 'b' to 0xFF6FB8FF.toInt()), dy = -1),
        WItem("dragon", "back", "Крылья дракона", 10, rows = listOf("g..................g", "gg................gg", "gGg..............gGg", "g.g..............g.g"), pal = mapOf('g' to 0xFF4F9A4F.toInt(), 'G' to 0xFF8FD46A.toInt()), dy = -1),
        WItem("cape", "back", "Плащ героя", 2, rows = listOf("r............r", "r............r", "rr...........r", "rr...........r", ".rr..........r", ".rrr........rr", "..rrrrrrrrrrr."), pal = mapOf('r' to 0xFFD6332B.toInt()), dy = 1),
        WItem("jetpack", "back", "Реактивный ранец", 7, rows = listOf("ss..............", "sss.............", "sss.............", ".o..............", ".y.............."), pal = mapOf('s' to 0xFF9AA0A8.toInt(), 'o' to 0xFFFF7A1A.toInt(), 'y' to 0xFFFFD34D.toInt()), dy = 1),
        WItem("backpack", "back", "Рюкзак", null, rows = listOf("bbb.", "bbbb", "bybb", "bbbb"), pal = mapOf('b' to 0xFF4F8A2E.toInt(), 'y' to 0xFFFFD34D.toInt()), dx = -7, dy = 1),
        WItem("foxtail", "back", "Лисий хвост", 1, rows = listOf("....o", "...oo", "..ooo", ".ooow", "oow.."), pal = mapOf('o' to 0xFFF08A24.toInt(), 'w' to 0xFFFFFFFF.toInt()), dx = -8, dy = 3),
        WItem("deviltail", "back", "Хвостик чертёнка", 3, rows = listOf("rr...", "rrr..", ".r...", "..rr.", "....r"), pal = mapOf('r' to 0xFFD6332B.toInt()), dx = -8, dy = 3),
    )

    private fun pattern() = listOf(
        WItem("stripes", "pattern", "Полоски", null, rows = listOf("D", ".")),
        WItem("tiger", "pattern", "Тигр", 2, rows = listOf("D...D.", ".D...D", "D...D.", "......")),
        WItem("giraffe", "pattern", "Жираф", 3, rows = listOf("DD....", "DD..D.", "....DD", ".D..DD")),
        WItem("dots", "pattern", "Горошек", null, rows = listOf("L...", "....", "..L.", "....")),
        WItem("belly", "pattern", "Пузико", null, rows = listOf("............", "............", "............", "....LLLL....", "...LLLLLL...", "...LLLLLL...", "....LLLL....")),
        WItem("checker", "pattern", "Шахматка", 1, rows = listOf("D.", ".D")),
        WItem("camo", "pattern", "Камуфляж", 4, rows = listOf("DD..LL..", "D..LLL.D", "..DD...D", "LL.DD.LL")),
        WItem("heartsp", "pattern", "Сердечки", 2, rows = listOf(".p.p...", "ppppp..", ".ppp...", "..p....", "......."), pal = mapOf('p' to 0xFFFF6FA8.toInt())),
        WItem("shadow", "pattern", "Закат", null, rows = listOf("L", "L", ".", ".", "D", "D", "D")),
        WItem("rainbow", "pattern", "Радуга", 6, rows = listOf("r", "o", "y", "g", "b", "i", "v"), pal = mapOf('r' to 0xFFEF5350.toInt(), 'o' to 0xFFFF9F43.toInt(), 'y' to 0xFFFFD34D.toInt(), 'g' to 0xFF66D17A.toInt(), 'b' to 0xFF4FA8E8.toInt(), 'i' to 0xFF5C6BD6.toInt(), 'v' to 0xFFA66BE0.toInt())),
        WItem("galaxy", "pattern", "Галактика", 9, rows = listOf("pppppPpppppp", "ppwppppppPpp", "pppppppwpppp", "pPpppppppppw", "ppppwppPpppp", "pppppppppwpp", "wppPpppppppp"), pal = mapOf('p' to 0xFF2A1F5C.toInt(), 'P' to 0xFF5A3FB0.toInt(), 'w' to 0xFFFFFFFF.toInt())),
        WItem("zombie", "pattern", "Зомби", 7, rows = listOf("............", ".gg.......k.", ".ggg.....kkk", "..g.......k.", "......ss....", "...gg.......", "..ggg......."), pal = mapOf('g' to 0xFF7FBF5A.toInt(), 's' to 0xFF3A2A2A.toInt(), 'k' to 0xFF3A2A2A.toInt())),
        WItem("robot", "pattern", "Робот", 8, rows = listOf("y..........y", "............", "............", "............", ".DDDDDDDDDD.", ".D.y.DD.y.D.", ".DDDDDDDDDD."), pal = mapOf('y' to 0xFFFFD34D.toInt())),
        WItem("lava", "pattern", "Лава", 11, rows = listOf("kokkkkokkkkk", "kkookkkkookk", "okkkoookkkko", "kkkkkkkokkkk", "kookkkkkkook", "kkkookkookkk", "okkkkookkkko"), pal = mapOf('k' to 0xFF3A1A12.toInt(), 'o' to 0xFFFF7A1A.toInt())),
    )

    private fun aura() = listOf(
        WItem("a_sparkle", "aura", "Блёстки", null, aura = WAura("rect", intArrayOf(0xFFFFD34D.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFF1A0.toInt()), "", 0.35f, false)),
        WItem("a_hearts", "aura", "Сердечки", 1, aura = WAura("heart", intArrayOf(), "", 0.6f, false)),
        WItem("a_fire", "aura", "Огонь", 5, aura = WAura("rect", intArrayOf(0xFFFF4A1A.toInt(), 0xFFFF9F43.toInt(), 0xFFFFD34D.toInt()), "", 0.12f, false)),
        WItem("a_bubbles", "aura", "Пузырьки", null, aura = WAura("ring", intArrayOf(0xFF9BD3FF.toInt(), 0xFFD6F0FF.toInt()), "", 0.4f, false)),
        WItem("a_notes", "aura", "Ноты", 1, aura = WAura("text", intArrayOf(0xFF7AB8FF.toInt(), 0xFFB28CFF.toInt(), 0xFF6BE3A2.toInt()), "♪♫", 0.6f, false)),
        WItem("a_snow", "aura", "Снегопад", 3, aura = WAura("rect", intArrayOf(0xFFFFFFFF.toInt()), "", 0.15f, true)),
        WItem("a_rainbow", "aura", "Радужный след", 8, aura = WAura("rect", intArrayOf(0xFFEF5350.toInt(), 0xFFFF9F43.toInt(), 0xFFFFD34D.toInt(), 0xFF66D17A.toInt(), 0xFF4FA8E8.toInt(), 0xFFA66BE0.toInt()), "", 0.1f, false)),
        WItem("a_stars", "aura", "Звёздочки", 2, aura = WAura("text", intArrayOf(0xFFFFD34D.toInt(), 0xFFFFF1A0.toInt(), 0xFF9BD3FF.toInt()), "✦★", 0.45f, false)),
        WItem("a_money", "aura", "Денежки", 10, aura = WAura("text", intArrayOf(0xFF66D17A.toInt(), 0xFF3FAE5A.toInt()), "\$", 0.4f, false)),
        WItem("a_leaves", "aura", "Листопад", 4, aura = WAura("rect", intArrayOf(0xFFE8A33A.toInt(), 0xFFD9622B.toInt(), 0xFF9BD45A.toInt()), "", 0.25f, true)),
        WItem("a_ghost", "aura", "Призрачная дымка", 6, aura = WAura("oval", intArrayOf(0xFFC9CCD6.toInt(), 0xFF9AA0B0.toInt()), "", 0.2f, false)),
        WItem("a_zap", "aura", "Молнии", 12, aura = WAura("text", intArrayOf(0xFFFFE14A.toInt(), 0xFF9BD3FF.toInt()), "ϟ", 0.3f, false)),
    )

    val ITEMS: Map<String, WItem> = (head() + face() + neck() + hand() + back() + pattern() + aura()).associateBy { it.key }
}
