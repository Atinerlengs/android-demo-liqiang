package application.android.com.zhaozehong.utils;

public class PrefixInfoData extends XlsData {

    String prefix;
    String info;
    String info_en;

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getInfo_en() {
        return info_en;
    }

    public void setInfo_en(String info_en) {
        this.info_en = info_en;
    }

    @Override
    public String toString() {
        return "prefix: " + prefix + ", info: "
                + info + ", info_en: " + info_en;
    }
}
