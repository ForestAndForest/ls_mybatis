package ls_mybatis;

import ls_mybatis.utils.JDBCUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public class Main {
    private static final long serialVersionUID = 1L;
    private static Properties properties = new Properties();

    public static void main(String[] args) throws IOException {
        List<Tag> tags = JDBCUtils.select(Tag.class, null);
        for (Tag tag : tags) {
            System.out.println(tag.getNoteId());
        }
    }
}
