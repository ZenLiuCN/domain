import cn.zenliu.domain.modeler.annotation.Gene;
import cn.zenliu.domain.modeler.prototype.Meta;

/**
 * @author Zen.Liu
 * @since 2023-04-21
 */
@Gene.Entity
public interface Some extends Meta.Object{
    Long getId();
}
