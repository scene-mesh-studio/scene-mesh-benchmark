package com.scene.mesh.benchmark.n.impl;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;
import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.type.AviatorNumber;
import com.googlecode.aviator.runtime.type.AviatorObject;
import com.googlecode.aviator.runtime.type.AviatorString;
import com.scene.mesh.benchmark.n.spec.ITemplateEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class AviatorTemplateEngine implements ITemplateEngine {

    private final Random random = new Random();
    private final Map<String, Expression> expressionCache = new HashMap<>();
    
    // 匹配 #{...} 格式的表达式
    private final Pattern EXPRESSION_PATTERN = Pattern.compile("#\\{([^}]+)\\}");

    @PostConstruct
    public void init() {
        // 全局注册函数
        AviatorEvaluator.addFunction(new RandomStringFunction());
        AviatorEvaluator.addFunction(new RandomFromFunction());
        AviatorEvaluator.addFunction(new RandomRangeFunction());
        
        log.info("Aviator 模板引擎初始化完成，已注册自定义函数");
    }

    @Override
    public String processTemplate(String template, Map<String, Object> context) {
        try {
            Map<String, Object> env = new HashMap<>(context);
            env.put("random", random);

            // 预处理：将数组字面量转换为字符串参数
            String processedTemplate = preprocessTemplate(template);
            
            // 使用正则表达式替换 #{...} 格式的表达式
            Matcher matcher = EXPRESSION_PATTERN.matcher(processedTemplate);
            StringBuffer result = new StringBuffer();
            
            while (matcher.find()) {
                String expression = matcher.group(1);
                Object value = evaluateExpression(expression, env);
                matcher.appendReplacement(result, Matcher.quoteReplacement(value.toString()));
            }
            matcher.appendTail(result);
            
            return result.toString();
        } catch (Exception e) {
            log.error("Aviator 模板处理失败: {}, 原始模板: {}", e.getMessage(), template);
            return template;
        }
    }
    
    /**
     * 预处理模板，将数组字面量转换为字符串参数
     */
    private String preprocessTemplate(String template) {
        // 将 ['option1','option2','option3'] 转换为 "option1,option2,option3"
        return template.replaceAll("\\['([^']+)'\\s*,\\s*'([^']+)'\\s*,\\s*'([^']+)'\\]", "\"$1,$2,$3\"")
                      .replaceAll("\\['([^']+)'\\s*,\\s*'([^']+)'\\]", "\"$1,$2\"")
                      .replaceAll("\\['([^']+)'\\]", "\"$1\"");
    }
    
    /**
     * 评估单个表达式
     */
    private Object evaluateExpression(String expression, Map<String, Object> env) {
        try {
            Expression expr = expressionCache.computeIfAbsent(expression, 
                exprStr -> AviatorEvaluator.compile(exprStr, true));
            return expr.execute(env);
        } catch (Exception e) {
            log.error("表达式评估失败: {}", expression, e);
            return "#{" + expression + "}"; // 返回原始表达式
        }
    }

    @Override
    public String getEngineName() {
        return "Aviator";
    }

    /**
     * 随机字符串生成函数
     */
    public class RandomStringFunction extends AbstractFunction {
        @Override
        public String getName() {
            return "randomString";
        }

        @Override
        public AviatorObject call(Map<String, Object> env,
                                  AviatorObject arg1,
                                  AviatorObject arg2) {

            int length = ((Number) arg1.getValue(env)).intValue();
            String charset = arg2.getValue(env).toString();

            String result = generateRandomString(length, charset);
            return new AviatorString(result);
        }
    }

    /**
     * 随机选择函数
     */
    public class RandomFromFunction extends AbstractFunction {
        @Override
        public String getName() {
            return "randomFrom";
        }

        @Override
        public AviatorObject call(Map<String, Object> env,
                                  AviatorObject arg1) {

            String options = arg1.getValue(env).toString();
            // 解析数组格式: ['option1','option2','option3']
            String[] optionArray = options.replaceAll("[\\[\\]']", "").split(",");
            for (int i = 0; i < optionArray.length; i++) {
                optionArray[i] = optionArray[i].trim();
            }

            String selectedOption = optionArray[random.nextInt(optionArray.length)];
            return new AviatorString(selectedOption);
        }
    }

    /**
     * 随机范围函数
     */
    public class RandomRangeFunction extends AbstractFunction {
        @Override
        public String getName() {
            return "randomRange";
        }

        @Override
        public AviatorObject call(Map<String, Object> env,
                                  AviatorObject arg1,
                                  AviatorObject arg2) {

            int min = ((Number) arg1.getValue(env)).intValue();
            int max = ((Number) arg2.getValue(env)).intValue();
            int randomValue = random.nextInt(max - min + 1) + min;

            return AviatorNumber.valueOf(randomValue);
        }
    }

    /**
     * 生成随机字符串
     */
    private String generateRandomString(int length, String charset) {
        StringBuilder sb = new StringBuilder();

        switch (charset.toLowerCase()) {
            case "chinese":
                // 生成常用中文字符 (更安全的范围)
                String commonChineseChars = "的一是了我不人在他有这个上们来到时大地为子中你说生国年着就那和要她出也得里后自以会家可下而过天去能对小多然于心学么之都好看起发当没成只如事把还用第样道想作种开美总从无情面最女但现前些所同日手又行意动方期它头经长儿回位分爱老因很给名法间斯知世什两次使身者被高已亲其进此话常与活正感见明问力理尔点文几定本公特做外孩相西果走将月十实向声车全信重三机工物气每并别真打太新比才便夫再书部水像眼等体却加电主界门利海受听表德少克代员许先口由死安写性马光白或住难望教命花结乐色更拉东神记处让母父应直字场平报友关放至张认接告入笑内英军候民岁往何度山觉路带万男边风解叫任金快原吃妈变通师立象数四失满战远格士音轻目条呢病始达深完今提求清王化空业思切怎非找片罗钱吗语元喜曾离飞科言干流欢约各即指合反题必该论交终林请医晚制球决传画保读运及则房早院量苦火布品近坐产答星精视五连司巴奇管类未朋且婚台夜青北队久乎越观落尽形影红爸百令周吧识步希亚术留市半热送兴造谈容极随演收首根讲整式取照办强石古华拿计您装似足双妻尼转诉米称丽客南领节衣站黑刻统断福城故历惊脸选包紧争另建维绝树系伤示愿持千史谁准联妇纪基买志静阿诗独复痛消社算义竟确酒需单治卡幸兰念举仅钟怕共毛句息功官待究跟穿室易游程号居考突皮哪费倒价图具刚脑永歌响商礼细专黄块脚味灵改据般破引食仍存众注笔甚某沉血备习校默务土微娘须试怀料调广苏显赛查密议底列富梦错座参八除跑亮假印设线温虽掉京初养香停际致阳纸李纳验助激够严证帝饭忘趣支春集丈木研班普导顿睡展跳获艺六波察群皇段急庭创区奥器谢弟店否害草排背止组州朝封睛板角况曲馆育忙质河续哥呼若推境遇雨标姐充围案伦护冷警贝著雪索剧啊船险烟依斗值帮汉慢佛肯闻唱沙局伯族低玩资屋击速顾泪洲团圣旁堂兵七露园牛哭旅街劳型烈姑陈莫鱼异抱宝权鲁简态级票怪寻杀律胜份汽右洋范床舞秘午登楼贵吸责例追较职属渐左录丝牙党继托赶章智冲叶胡吉卖坚喝肉遗救修松临藏担戏善卫药悲敢靠伊村戴词森耳差短祖云规窗散迷油旧适乡架恩投弹铁博雷府压超负勒杂醒洗采毫嘴毕九冰既状乱景席珍童顶派素脱农疑练野按犯拍征坏骨余承置臓彩灯巨琴免环姆暗换技翻束增忍餐洛塞缺忆判欧层付阵玛批岛项狗休懂武革良恶恋委拥娜妙探呀营退摇弄桌熟诺宣银势奖宫忽套康供优课鸟喊降夏困刘罪亡鞋健模败伴守挥鲜财孤枪禁恐伙杰迹妹藜遍盖副坦牌江顺秋萨菜划授归浪听凡预奶雄升碃编典袋莱含盛济蒙棋端腿招释介烧误";
                for (int i = 0; i < length; i++) {
                    sb.append(commonChineseChars.charAt(random.nextInt(commonChineseChars.length())));
                }
                break;
            case "english":
                // 生成英文字符
                String englishChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
                for (int i = 0; i < length; i++) {
                    sb.append(englishChars.charAt(random.nextInt(englishChars.length())));
                }
                break;
            case "alphanumeric":
                // 生成字母数字字符
                String alphanumericChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
                for (int i = 0; i < length; i++) {
                    sb.append(alphanumericChars.charAt(random.nextInt(alphanumericChars.length())));
                }
                break;
            default:
                // 默认生成数字
                for (int i = 0; i < length; i++) {
                    sb.append(random.nextInt(10));
                }
                break;
        }

        return sb.toString();
    }
}