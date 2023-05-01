package cn.zenliu.domain.modeler.util;

import cn.zenliu.domain.modeler.annotation.Info;
import cn.zenliu.domain.modeler.prototype.Meta;

@Info.Type({0x0, 0x1, 0x12, 0x73, 0x6f, 0x6d, 0x65, 0x2e, 0x70, 0x61, 0x63, 0x6b, 0x2e, 0x4d, 0x65, 0x74, 0x61, 0x54, 0x65, 0x73, 0x74, 0x0, 0x0, 0x0, 0x0})
public interface SampleFields extends Meta.Fields {

    String COUNT = "count";

    Class COUNT_TYPE = Object.class;

    String PAYLOADS = "payloads";

    @Info.Type({0x0, 0x0, 0x1, 0x0, 0x1, 0xd, 0x6a, 0x61, 0x76, 0x61, 0x2e, 0x75, 0x74, 0x69, 0x6c, 0x2e, 0x4d, 0x61, 0x70, 0x0, 0x0, 0x0, 0x0, 0x2, 0x0, 0x1, 0x10, 0x6a, 0x61, 0x76, 0x61, 0x2e, 0x6c, 0x61, 0x6e, 0x67, 0x2e, 0x53, 0x74, 0x72, 0x69, 0x6e, 0x67, 0x0, 0x0, 0x0, 0x0, 0x0, 0x1, 0x10, 0x6a, 0x61, 0x76, 0x61, 0x2e, 0x6c, 0x61, 0x6e, 0x67, 0x2e, 0x53, 0x74, 0x72, 0x69, 0x6e, 0x67, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0})
    Class PAYLOADS_TYPE = java.util.Map.class;
}
