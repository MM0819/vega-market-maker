package com.vega.protocol.model;

import com.vega.protocol.constant.DataType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class NetworkParameter extends UniqueItem {
    private String id;
    private String value;
    private DataType type;
}