package com.wly.workorder.common;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {
  private long total;
  private int pageNum;
  private int pageSize;
  private List<T> records;

  public static <T> PageResult<T> of(long total, int pageNum, int pageSize, List<T> records) {
    return new PageResult<>(total, pageNum, pageSize, records);
  }
}
