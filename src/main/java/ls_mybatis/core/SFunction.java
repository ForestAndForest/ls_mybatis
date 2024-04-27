package ls_mybatis.core;

import java.io.Serializable;
import java.util.function.Function;

/**
 * @author 29002
 */
@FunctionalInterface
public interface SFunction<T, R> extends Function<T, R>, Serializable {
}
