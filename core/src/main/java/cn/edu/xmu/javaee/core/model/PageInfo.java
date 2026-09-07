package cn.edu.xmu.javaee.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString(doNotUseGetters = true)
public class PageInfo<T> implements Serializable {
    private Integer totalPages;
    private Integer number;
    private Integer size;
    private Boolean empty;
    private List<T> content;
}
