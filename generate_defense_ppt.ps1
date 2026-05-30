Add-Type -AssemblyName System.IO.Compression.FileSystem

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$out = Join-Path $root "数计学院志愿服务小程序-答辩展示.pptx"
$work = Join-Path $root ".pptx_build"

if (Test-Path -LiteralPath $work) {
    Remove-Item -LiteralPath $work -Recurse -Force
}
New-Item -ItemType Directory -Path $work | Out-Null

function Ensure-Dir($path) {
    if (!(Test-Path -LiteralPath $path)) {
        New-Item -ItemType Directory -Path $path | Out-Null
    }
}

function Write-Utf8($path, $content) {
    $encoding = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($path, $content, $encoding)
}

function X($text) {
    if ($null -eq $text) { return "" }
    return [System.Security.SecurityElement]::Escape([string]$text)
}

function Text-Run($text, $size = 2400, $bold = $false, $color = "172033") {
    $b = if ($bold) { '<a:b/>' } else { '' }
    return "<a:r><a:rPr lang=""zh-CN"" sz=""$size"">$b<a:solidFill><a:srgbClr val=""$color""/></a:solidFill></a:rPr><a:t>$(X $text)</a:t></a:r>"
}

function Shape-Xml($id, $x, $y, $cx, $cy, $text, $size = 2400, $bold = $false, $color = "172033") {
    return @"
<p:sp>
  <p:nvSpPr><p:cNvPr id="$id" name="TextBox $id"/><p:cNvSpPr txBox="1"/><p:nvPr/></p:nvSpPr>
  <p:spPr>
    <a:xfrm><a:off x="$x" y="$y"/><a:ext cx="$cx" cy="$cy"/></a:xfrm>
    <a:prstGeom prst="rect"><a:avLst/></a:prstGeom>
    <a:noFill/>
  </p:spPr>
  <p:txBody>
    <a:bodyPr wrap="square" anchor="t"/>
    <a:lstStyle/>
    <a:p>$(Text-Run $text $size $bold $color)</a:p>
  </p:txBody>
</p:sp>
"@
}

function Bullet-Xml($id, $x, $y, $cx, $cy, $items) {
    $paras = ""
    foreach ($item in $items) {
        $paras += "<a:p><a:pPr marL=""342900"" indent=""-171450""><a:buChar char=""•""/></a:pPr>$(Text-Run $item 2150 $false "344054")</a:p>"
    }
    return @"
<p:sp>
  <p:nvSpPr><p:cNvPr id="$id" name="BulletBox $id"/><p:cNvSpPr txBox="1"/><p:nvPr/></p:nvSpPr>
  <p:spPr>
    <a:xfrm><a:off x="$x" y="$y"/><a:ext cx="$cx" cy="$cy"/></a:xfrm>
    <a:prstGeom prst="rect"><a:avLst/></a:prstGeom>
    <a:noFill/>
  </p:spPr>
  <p:txBody>
    <a:bodyPr wrap="square" anchor="t"/>
    <a:lstStyle/>
    $paras
  </p:txBody>
</p:sp>
"@
}

function Accent-Rect($id, $x, $y, $cx, $cy, $color = "0F766E") {
    return @"
<p:sp>
  <p:nvSpPr><p:cNvPr id="$id" name="Accent $id"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr>
  <p:spPr>
    <a:xfrm><a:off x="$x" y="$y"/><a:ext cx="$cx" cy="$cy"/></a:xfrm>
    <a:prstGeom prst="rect"><a:avLst/></a:prstGeom>
    <a:solidFill><a:srgbClr val="$color"/></a:solidFill>
    <a:ln><a:noFill/></a:ln>
  </p:spPr>
</p:sp>
"@
}

function Card-Xml($id, $x, $y, $cx, $cy, $title, $body, $color = "0F766E") {
    return @"
<p:sp>
  <p:nvSpPr><p:cNvPr id="$id" name="Card $id"/><p:cNvSpPr txBox="1"/><p:nvPr/></p:nvSpPr>
  <p:spPr>
    <a:xfrm><a:off x="$x" y="$y"/><a:ext cx="$cx" cy="$cy"/></a:xfrm>
    <a:prstGeom prst="roundRect"><a:avLst/></a:prstGeom>
    <a:solidFill><a:srgbClr val="F8FAFC"/></a:solidFill>
    <a:ln w="9525"><a:solidFill><a:srgbClr val="E5E7EB"/></a:solidFill></a:ln>
  </p:spPr>
  <p:txBody>
    <a:bodyPr wrap="square" lIns="180000" tIns="150000" rIns="180000" bIns="120000"/>
    <a:lstStyle/>
    <a:p>$(Text-Run $title 2300 $true $color)</a:p>
    <a:p>$(Text-Run $body 1750 $false "475467")</a:p>
  </p:txBody>
</p:sp>
"@
}

$slides = @(
    @{
        Title = "数计学院志愿服务小程序"
        Subtitle = "答辩展示 | 活动发布、报名审核、签到管理、智能匹配与 AI 服务闭环"
        Bullets = @("项目类型：微信小程序 + Spring Boot 后端 + MySQL + pgvector", "面向对象：学院管理员与学生志愿者", "核心目标：提升志愿活动管理效率与学生参与体验")
    },
    @{
        Title = "项目背景与痛点"
        Subtitle = "传统志愿服务管理存在分散、低效、难统计的问题"
        Bullets = @("活动通知、报名、审核、签到和服务时长统计分散在多个渠道", "管理员难以及时掌握活动热度、人员匹配和签到异常", "学生缺少统一入口查看活动、成长积分、信用档案和服务记录", "活动结束后的新闻宣传、总结报告和数据沉淀成本较高")
    },
    @{
        Title = "系统总体架构"
        Subtitle = "前端原生小程序，后端 REST API，数据层区分业务库与向量库"
        Bullets = @("微信小程序：页面展示、活动报名、消息中心、AI 对话与个人中心", "Spring Boot 后端：登录鉴权、活动管理、报名审核、签到、统计、AI 接口", "MySQL：用户、活动、报名、签到、公告、新闻、报告等业务数据", "PostgreSQL + pgvector：规则文件切片向量化，支撑 RAG 规则问答")
    },
    @{
        Title = "用户端核心功能"
        Subtitle = "围绕学生参与志愿服务的完整流程设计"
        Bullets = @("首页：活动推荐、最新新闻轮播、服务数据概览、消息红点", "活动大厅：按关键词、类别、状态筛选活动", "活动详情：查看名额、时间、地点、技能要求、联系人和报名要求", "报名与签到：在线报名、审核状态查看、定位签到、异常申请", "个人中心：服务时长、积分、信用档案、勋章墙、资料编辑")
    },
    @{
        Title = "管理员端核心功能"
        Subtitle = "覆盖活动运营、人员管理和数据决策"
        Bullets = @("活动发布与编辑：配置活动时间、报名时间、签到地点、招募人数和技能要求", "报名审核：查看报名人员资料、匹配情况、历史评价并执行同意/拒绝", "签到管理：查看签到统计、签到列表、手动补签和异常处理", "志愿者库：按姓名、专业、技能筛选，查看志愿者详情和服务数据", "公告、规则文件、活动新闻统一管理")
    },
    @{
        Title = "AI 能力亮点"
        Subtitle = "把 AI 融入活动创建、参与推荐、问答和总结报告"
        Bullets = @("AI 志愿助手：根据学生问题推荐活动并解释原因", "活动智能分析：分析活动与用户技能、时间、历史参与情况的匹配度", "AI 一键生成活动：生成标题、简介、要求、技能建议和封面", "AI 新闻稿生成：活动结束后自动生成活动风采稿", "AI Report Center：生成个人成长报告和管理员运营分析报告")
    },
    @{
        Title = "智能匹配机制"
        Subtitle = "帮助管理员更快找到合适志愿者"
        Bullets = @("综合志愿者技能标签、可服务时间、信用分、历史服务记录进行推荐", "活动详情页可直接查看推荐志愿者 Top 列表", "报名人员列表展示匹配分、匹配理由和 AI 历史评价总结", "管理员可从志愿者详情发起私聊或发送活动邀请")
    },
    @{
        Title = "后台数据中心"
        Subtitle = "新增时间范围筛选与活动类别环形图"
        Bullets = @("支持近30天、本月、全部和自定义日期范围统计", "统计活动总数、参与志愿者、服务时长、待审核报名", "活动发布趋势、技能分布、签到率、服务时长排行、积分排行", "活动类别统计以环形饼图展示，便于答辩时直观看出服务类型分布")
    },
    @{
        Title = "消息协同与通知闭环"
        Subtitle = "减少信息遗漏，连接管理员和志愿者"
        Bullets = @("消息中心聚合私聊消息、活动邀请和系统通知", "报名审核、公告发布、新闻发布、签到异常均可产生通知", "聊天支持文字、图片、活动邀请、已读和未读统计", "管理员可从活动报名列表或志愿者详情直接联系学生")
    },
    @{
        Title = "数据清理与演示数据"
        Subtitle = "已准备干净、可复现的答辩演示数据"
        Bullets = @("MySQL data.sql：清空脏业务数据并导入标准志愿服务数据", "包含活动、报名、签到、补签异常、新闻轮播、公告、规则文件、消息和 AI 报告", "PostgreSQL clean_vector_data.sql：清空 pgvector 规则文件切片，避免旧内容干扰 RAG 问答", "测试账号保留在文档中，登录页已移除账号提示")
    },
    @{
        Title = "演示流程建议"
        Subtitle = "按学生端、管理员端、AI 亮点三段演示"
        Bullets = @("学生端：登录 -> 首页轮播 -> 活动大厅 -> 活动详情 -> 报名/签到 -> 个人中心", "管理员端：后台数据中心 -> 时间范围筛选 -> 饼图 -> 发布活动 -> 审核报名 -> 签到管理", "AI 亮点：AI 生成活动、活动智能分析、AI 志愿助手、AI 报告导出", "数据闭环：新闻发布、消息通知、积分信用、服务报告")
    },
    @{
        Title = "项目总结"
        Subtitle = "从流程管理走向智能化志愿服务平台"
        Bullets = @("实现了活动发布、报名、审核、签到、评价、统计的完整业务闭环", "通过志愿者库和智能匹配提升管理员调度效率", "通过 AI 助手、AI 生成和 AI 报告提升系统创新性", "后续可扩展校级多组织管理、电子证书、地图排班和更细粒度的数据分析")
    }
)

Ensure-Dir (Join-Path $work "_rels")
Ensure-Dir (Join-Path $work "docProps")
Ensure-Dir (Join-Path $work "ppt")
Ensure-Dir (Join-Path $work "ppt\_rels")
Ensure-Dir (Join-Path $work "ppt\slides")
Ensure-Dir (Join-Path $work "ppt\slides\_rels")
Ensure-Dir (Join-Path $work "ppt\theme")

$contentTypes = @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
  <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
  <Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>
  <Override PartName="/ppt/theme/theme1.xml" ContentType="application/vnd.openxmlformats-officedocument.theme+xml"/>
"@
for ($i = 1; $i -le $slides.Count; $i++) {
    $contentTypes += "  <Override PartName=""/ppt/slides/slide$i.xml"" ContentType=""application/vnd.openxmlformats-officedocument.presentationml.slide+xml""/>`n"
}
$contentTypes += "</Types>"
Write-Utf8 (Join-Path $work "[Content_Types].xml") $contentTypes

Write-Utf8 (Join-Path $work "_rels\.rels") @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="ppt/presentation.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
</Relationships>
"@

Write-Utf8 (Join-Path $work "docProps\core.xml") @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:dcmitype="http://purl.org/dc/dcmitype/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <dc:title>数计学院志愿服务小程序答辩展示</dc:title>
  <dc:creator>Codex</dc:creator>
  <cp:lastModifiedBy>Codex</cp:lastModifiedBy>
  <dcterms:created xsi:type="dcterms:W3CDTF">2026-05-30T00:00:00Z</dcterms:created>
  <dcterms:modified xsi:type="dcterms:W3CDTF">2026-05-30T00:00:00Z</dcterms:modified>
</cp:coreProperties>
"@

Write-Utf8 (Join-Path $work "docProps\app.xml") @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">
  <Application>Microsoft PowerPoint</Application>
  <PresentationFormat>宽屏</PresentationFormat>
  <Slides>$($slides.Count)</Slides>
</Properties>
"@

$slideIdList = ""
$presRels = @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rIdTheme" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme" Target="theme/theme1.xml"/>
"@
for ($i = 1; $i -le $slides.Count; $i++) {
    $rid = "rId$i"
    $sid = 255 + $i
    $slideIdList += "    <p:sldId id=""$sid"" r:id=""$rid""/>`n"
    $presRels += "  <Relationship Id=""$rid"" Type=""http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide"" Target=""slides/slide$i.xml""/>`n"
}
$presRels += "</Relationships>"
Write-Utf8 (Join-Path $work "ppt\_rels\presentation.xml.rels") $presRels

Write-Utf8 (Join-Path $work "ppt\presentation.xml") @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:presentation xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:sldIdLst>
$slideIdList  </p:sldIdLst>
  <p:sldSz cx="12192000" cy="6858000" type="wide"/>
  <p:notesSz cx="6858000" cy="9144000"/>
</p:presentation>
"@

Write-Utf8 (Join-Path $work "ppt\theme\theme1.xml") @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<a:theme xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" name="Volunteer Theme">
  <a:themeElements>
    <a:clrScheme name="Volunteer">
      <a:dk1><a:srgbClr val="172033"/></a:dk1><a:lt1><a:srgbClr val="FFFFFF"/></a:lt1>
      <a:dk2><a:srgbClr val="0F766E"/></a:dk2><a:lt2><a:srgbClr val="F8FAFC"/></a:lt2>
      <a:accent1><a:srgbClr val="0F766E"/></a:accent1><a:accent2><a:srgbClr val="2563EB"/></a:accent2>
      <a:accent3><a:srgbClr val="F59E0B"/></a:accent3><a:accent4><a:srgbClr val="EF4444"/></a:accent4>
      <a:accent5><a:srgbClr val="8B5CF6"/></a:accent5><a:accent6><a:srgbClr val="14B8A6"/></a:accent6>
      <a:hlink><a:srgbClr val="2563EB"/></a:hlink><a:folHlink><a:srgbClr val="8B5CF6"/></a:folHlink>
    </a:clrScheme>
    <a:fontScheme name="VolunteerFonts">
      <a:majorFont><a:latin typeface="Microsoft YaHei"/><a:ea typeface="Microsoft YaHei"/></a:majorFont>
      <a:minorFont><a:latin typeface="Microsoft YaHei"/><a:ea typeface="Microsoft YaHei"/></a:minorFont>
    </a:fontScheme>
    <a:fmtScheme name="VolunteerFmt"><a:fillStyleLst/><a:lnStyleLst/><a:effectStyleLst/><a:bgFillStyleLst/></a:fmtScheme>
  </a:themeElements>
</a:theme>
"@

for ($i = 1; $i -le $slides.Count; $i++) {
    $s = $slides[$i - 1]
    $shapes = ""
    $shapes += Accent-Rect 2 0 0 12192000 300000 "0F766E"
    $shapes += Accent-Rect 3 0 6558000 12192000 300000 "0F766E"
    $shapes += Shape-Xml 4 650000 620000 10800000 720000 $s.Title 3600 $true "0F766E"
    $shapes += Shape-Xml 5 650000 1330000 10800000 430000 $s.Subtitle 1900 $false "667085"
    $shapes += Bullet-Xml 6 850000 2050000 10450000 3150000 $s.Bullets
    $shapes += Card-Xml 7 850000 5500000 2200000 520000 "第 $i 页" "数计学院志愿服务小程序" "2563EB"
    $slideXml = @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sld xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:cSld>
    <p:bg><p:bgPr><a:solidFill><a:srgbClr val="FFFFFF"/></a:solidFill></p:bgPr></p:bg>
    <p:spTree>
      <p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>
      <p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr>
      $shapes
    </p:spTree>
  </p:cSld>
  <p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>
</p:sld>
"@
    Write-Utf8 (Join-Path $work "ppt\slides\slide$i.xml") $slideXml
    Write-Utf8 (Join-Path $work "ppt\slides\_rels\slide$i.xml.rels") @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"/>
"@
}

if (Test-Path -LiteralPath $out) {
    Remove-Item -LiteralPath $out -Force
}
[System.IO.Compression.ZipFile]::CreateFromDirectory($work, $out)
Remove-Item -LiteralPath $work -Recurse -Force

Write-Output $out
