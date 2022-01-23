package com.mxsimulator.saf;

import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class SafFile {
    private int byteCount;
    private String path;
    private byte[] bytes;
}
