//School of Informatics Xiamen University, GPL-3.0 license
package cn.edu.xmu.javaee.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 简单用户
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserToken implements Serializable {

    public UserToken(Long id, String name, Long departId, Integer userLevel) {
        this.id = id;
        this.name = name;
        this.departId = departId;
        this.userLevel = userLevel;
    }

    private Long id;
    /**
     * 用户名
     */
    private String name;
    /**
     * 部门id
     */
    private Long departId;

    /**
     * 过期时间
     */
    private Date expireTime;
    /**
     * 用户级别
     */
    private Integer userLevel;

}
