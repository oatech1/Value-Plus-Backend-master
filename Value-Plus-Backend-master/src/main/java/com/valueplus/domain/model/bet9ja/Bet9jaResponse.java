package com.valueplus.domain.model.bet9ja;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;

@Data
@RequiredArgsConstructor
public class Bet9jaResponse {
    private Params params;
    private ArrayList<Results> results;
}
