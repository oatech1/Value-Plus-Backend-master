
package com.valueplus.domain.model;

import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SuperAgentSummary {
    String superAgentName;
    int totalActiveAgent;
    int totalAgent;
}