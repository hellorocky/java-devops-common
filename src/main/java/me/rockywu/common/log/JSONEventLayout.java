package me.rockywu.common.log;

/**
 * @author RockyWu
 * @date 2018/11/8
 */
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.LayoutBase;
import com.cwbase.logback.AdditionalField;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JSONEventLayout extends LayoutBase<ILoggingEvent> {
    private final int DEFAULT_SIZE = 256;
    private final int UPPER_LIMIT = 2048;
    private static final char DBL_QUOTE = '"';
    private static final char COMMA = ',';
    private StringBuilder buf = new StringBuilder(256);
    private DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SZ");
    private Pattern MDC_VAR_PATTERN = Pattern.compile("\\@\\{([^}]*)\\}");
    private boolean locationInfo = true;
    private int callerStackIdx = 0;
    private boolean properties = false;
    String source;
    String sourceHost;
    String sourcePath;
    List<String> tags;
    List<AdditionalField> additionalFields;
    String type;

    public JSONEventLayout() {
        try {
            this.setSource(InetAddress.getLocalHost().getHostAddress());
            this.setSourceHost(InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException var2) {}

    }

    @Override
    public void start() {
        super.start();
    }

    public void setLocationInfo(boolean flag) {
        this.locationInfo = flag;
    }

    public boolean getLocationInfo() {
        return this.locationInfo;
    }

    public void setProperties(boolean flag) {
        this.properties = flag;
    }

    public boolean getProperties() {
        return this.properties;
    }

    //    @Override
    public synchronized String doLayout(ILoggingEvent event) {
        if (this.buf.capacity() > 2048) {
            this.buf = new StringBuilder(256);
        } else {
            this.buf.setLength(0);
        }

        Map<String, String> mdc = event.getMDCPropertyMap();
        this.buf.append("{");
        this.appendKeyValue(this.buf, "source", this.source, mdc);
        this.buf.append(',');
        this.appendKeyValue(this.buf, "host", this.sourceHost, mdc);
        this.buf.append(',');
        this.appendKeyValue(this.buf, "path", this.sourcePath, mdc);
        this.buf.append(',');
        this.appendKeyValue(this.buf, "type", this.type, mdc);
        this.buf.append(',');
        this.appendKeyValue(this.buf, "tags", this.tags, mdc);
        this.buf.append(',');
        this.appendKeyValue(this.buf, "msg", (String)event.getFormattedMessage(), (Map)null);
        this.buf.append(',');
        this.appendKeyValue(this.buf, "time", (String)this.df.format(new Date(event.getTimeStamp())), (Map)null);
        this.buf.append(',');
        this.appendKeyValue(this.buf, "logger", (String)event.getLoggerName(), (Map)null);
        this.buf.append(',');
        this.appendKeyValue(this.buf, "level", (String)event.getLevel().toString(), (Map)null);
        this.buf.append(',');
        this.appendKeyValue(this.buf, "thread", (String)event.getThreadName(), (Map)null);
        this.buf.append(',');
        this.appendKeyValue(this.buf, "level", (String)event.getLevel().toString(), (Map)null);
        IThrowableProxy tp = event.getThrowableProxy();
        if (tp != null) {
            this.buf.append(',');
            String throwable = ThrowableProxyUtil.asString(tp);
            this.appendKeyValue(this.buf, "throwable", (String)throwable, (Map)null);
        }

        if (this.locationInfo) {
            StackTraceElement[] callerDataArray = event.getCallerData();
            if (callerDataArray != null && callerDataArray.length > this.callerStackIdx) {
                this.buf.append(',');
                this.buf.append("\"location\":{");
                StackTraceElement immediateCallerData = callerDataArray[this.callerStackIdx];
                this.appendKeyValue(this.buf, "class", (String)immediateCallerData.getClassName(), (Map)null);
                this.buf.append(',');
                this.appendKeyValue(this.buf, "method", (String)immediateCallerData.getMethodName(), (Map)null);
                this.buf.append(',');
                this.appendKeyValue(this.buf, "file", (String)immediateCallerData.getFileName(), (Map)null);
                this.buf.append(',');
                this.appendKeyValue(this.buf, "line", (String)Integer.toString(immediateCallerData.getLineNumber()), (Map)null);
                this.buf.append("}");
            }
        }

        if (this.properties) {
            Map<String, String> propertyMap = event.getMDCPropertyMap();
            if (propertyMap != null && propertyMap.size() != 0) {
                Set<Entry<String, String>> entrySet = propertyMap.entrySet();
                this.buf.append(',');
                this.buf.append("\"properties\":{");
                Iterator i = entrySet.iterator();

                while(i.hasNext()) {
                    Entry<String, String> entry = (Entry)i.next();
                    this.appendKeyValue(this.buf, (String)entry.getKey(), (String)((String)entry.getValue()), (Map)null);
                    if (i.hasNext()) {
                        this.buf.append(',');
                    }
                }

                this.buf.append("}");
            }
        }

        if (this.additionalFields != null) {
            Iterator var10 = this.additionalFields.iterator();

            while(var10.hasNext()) {
                AdditionalField field = (AdditionalField)var10.next();
                this.buf.append(',');
                this.appendKeyValue(this.buf, field.getKey(), field.getValue(), mdc);
            }
        }

        this.buf.append("}");
        return this.buf.toString();
    }

    private void appendKeyValue(StringBuilder buf, String key, String value, Map<String, String> mdc) {
        if (value != null) {
            buf.append('"');
            buf.append(this.escape(key));
            buf.append('"');
            buf.append(':');
            buf.append('"');
            buf.append(this.escape(this.mdcSubst(value, mdc)));
            buf.append('"');
        } else {
            buf.append('"');
            buf.append(this.escape(key));
            buf.append('"');
            buf.append(':');
            buf.append("null");
        }

    }

    private void appendKeyValue(StringBuilder buf, String key, List<String> values, Map<String, String> mdc) {
        buf.append('"');
        buf.append(this.escape(key));
        buf.append('"');
        buf.append(':');
        buf.append('[');
        if (values != null) {
            Iterator i = values.iterator();

            while(i.hasNext()) {
                String v = (String)i.next();
                buf.append('"');
                buf.append(this.escape(this.mdcSubst(v, mdc)));
                buf.append('"');
                if (i.hasNext()) {
                    buf.append(',');
                }
            }
        }

        buf.append(']');
    }

    private String mdcSubst(String v, Map<String, String> mdc) {
        if (mdc != null && v != null && v.contains("@{")) {
            Matcher m = this.MDC_VAR_PATTERN.matcher(v);

            StringBuffer sb;
            String val;
            for(sb = new StringBuffer(v.length()); m.find(); m.appendReplacement(sb, Matcher.quoteReplacement(val))) {
                val = (String)mdc.get(m.group(1));
                if (val == null) {
                    val = m.group(1) + "_NOT_FOUND";
                }
            }

            m.appendTail(sb);
            return sb.toString();
        } else {
            return v;
        }
    }

    private String escape(String s) {
        if (s == null) {
            return null;
        } else {
            StringBuilder sb = new StringBuilder();

            for(int i = 0; i < s.length(); ++i) {
                char ch = s.charAt(i);
                switch(ch) {
                    case '\b':
                        sb.append("\\b");
                        continue;
                    case '\t':
                        sb.append("\\t");
                        continue;
                    case '\n':
                        sb.append("\\n");
                        continue;
                    case '\f':
                        sb.append("\\f");
                        continue;
                    case '\r':
                        sb.append("\\r");
                        continue;
                    case '"':
                        sb.append("\\\"");
                        continue;
                    case '/':
                        sb.append("\\/");
                        continue;
                    case '\\':
                        sb.append("\\\\");
                        continue;
                }

                if (ch >= 0 && ch <= 31) {
                    String ss = Integer.toHexString(ch);
                    sb.append("\\u");

                    for(int k = 0; k < 4 - ss.length(); ++k) {
                        sb.append('0');
                    }

                    sb.append(ss.toUpperCase());
                } else {
                    sb.append(ch);
                }
            }

            return sb.toString();
        }
    }

    @Override
    public String getContentType() {
        return "application/json";
    }

    public String getSource() {
        return this.source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceHost() {
        return this.sourceHost;
    }

    public void setSourceHost(String sourceHost) {
        this.sourceHost = sourceHost;
    }

    public String getSourcePath() {
        return this.sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public List<String> getTags() {
        return this.tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getCallerStackIdx() {
        return this.callerStackIdx;
    }

    public void setCallerStackIdx(int callerStackIdx) {
        this.callerStackIdx = callerStackIdx;
    }

    public void addAdditionalField(AdditionalField p) {
        if (this.additionalFields == null) {
            this.additionalFields = new ArrayList();
        }

        this.additionalFields.add(p);
    }
}

