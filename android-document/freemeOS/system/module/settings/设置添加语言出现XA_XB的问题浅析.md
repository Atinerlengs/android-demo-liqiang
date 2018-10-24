### 问题描述：
在设置界面语言和输入法选项里面点击“添加语言”选项，在其“建议语言”列表下面会出现“English(XA)”,"XXX(XB)"两个推荐语言，但是点击添加之后，切换到该语言，系统语言没有发生变化。
### 问题原因：

- 当我们点击进入语言界面时，会执行LocaleStore的fillCache方法：

```
    public static void fillCache(Context context) {
        if (sFullyInitialized) {
            return;
        }

        Set<String> simCountries = getSimCountries(context);

        for (String localeId : LocalePicker.getSupportedLocales(context)) {
            if (localeId.isEmpty()) {
                throw new IllformedLocaleException("Bad locale entry in locale_config.xml");
            }
            LocaleInfo li = new LocaleInfo(localeId);
            if (simCountries.contains(li.getLocale().getCountry())) {
                li.mSuggestionFlags |= LocaleInfo.SUGGESTION_TYPE_SIM;
            }
            sLocaleCache.put(li.getId(), li);
            final Locale parent = li.getParent();
            if (parent != null) {
                String parentId = parent.toLanguageTag();
                if (!sLocaleCache.containsKey(parentId)) {
                    sLocaleCache.put(parentId, new LocaleInfo(parent));
                }
            }
        }

        boolean isInDeveloperMode = Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0;
        for (String localeId : LocalePicker.getPseudoLocales()) {
            LocaleInfo li = getLocaleInfo(Locale.forLanguageTag(localeId));
            if (isInDeveloperMode) {
                li.setTranslated(true);
                li.mIsPseudo = true;
                li.mSuggestionFlags |= LocaleInfo.SUGGESTION_TYPE_SIM;
            } else {
                sLocaleCache.remove(li.getId());
            }
        }
        ...
    }
```

这个方法会读取locale_config.xml里面配置的语言，然后添加到sLocaleCache中。下面会读取Settings.Global.DEVELOPMENT_SETTINGS_ENABLED值，如果为true，则会添加两个pseudoLocales

```
private static final String[] pseudoLocales = { "en-XA", "ar-XB" };
```

并且在getLocaleInfo中，如果这两个语言不在sLocaleCache中，则会添加进去

```
    public static LocaleInfo getLocaleInfo(Locale locale) {
        String id = locale.toLanguageTag();
        LocaleInfo result;
        if (!sLocaleCache.containsKey(id)) {
            result = new LocaleInfo(locale);
            sLocaleCache.put(id, result);
        } else {
            result = sLocaleCache.get(id);
        }
        return result;
    }
```

但是如果想要在“建议语言”列表中显示，则需要一个flag标识：LocaleInfo.SUGGESTION_TYPE_SIM。从代码上面可以看出，首选是和sim卡相匹配的国家语言。如果是处于DeveloperMode，则pseudoLocales也会被标识。

- 在build目录下面languages_full.mk中定义需要编译的语言列表

```
PRODUCT_LOCALES := en_US en_AU en_IN fr_FR it_IT es_ES et_EE de_DE nl_NL cs_CZ pl_PL ja_JP zh_TW zh_CN zh_HK ru_RU ko_KR nb_NO es_US da_DK el_GR tr_TR pt_PT pt_BR sv_SE bg_BG ca_ES en_GB     fi_FI hi_IN hr_HR hu_HU in_ID iw_IL lt_LT lv_LV ro_RO sk_SK sl_SI sr_RS uk_UA vi_VN tl_PH ar_EG fa_IR th_TH sw_TZ ms_MY af_ZA zu_ZA am_ET en_XA ar_XB fr_CA km_KH lo_LA ne_NP si_LK mn_MN     hy_AM az_AZ ka_GE my_MM mr_IN ml_IN is_IS mk_MK ky_KG eu_ES gl_ES bn_BD ta_IN kn_IN te_IN uz_UZ ur_PK kk_KZ sq_AL gu_IN pa_IN be_BY bs_BA 
```

这里面包含了en_XA,ar_XB两个语言

- 但是在我们device目录下面获取的PRODUCT_LOCALES不包含en_XA，ar_XB

```
PRODUCT_LOCALES := en_US zh_CN zh_TW es_ES pt_BR ru_RU fr_FR de_DE tr_TR vi_VN ms_MY in_ID th_TH it_IT ar_EG hi_IN bn_IN ur_PK fa_IR pt_PT nl_NL el_GR hu_HU tl_PH ro_RO cs_CZ ko_KR km_    KH iw_IL my_MM pl_PL es_US bg_BG hr_HR lv_LV lt_LT sk_SK uk_UA de_AT da_DK fi_FI nb_NO sv_SE en_GB hy_AM zh_HK et_EE ja_JP kk_KZ sr_RS sl_SI ca_ES
```

### 解决方案

1. 在代码中将en_XA,ar_XB两个语言去掉，这样在语言列表添加界面“推荐语言”列表中不会出现
2. 将en_XA,ar_XB两种语言编译到系统中，这样切换之后显示不同的语言
