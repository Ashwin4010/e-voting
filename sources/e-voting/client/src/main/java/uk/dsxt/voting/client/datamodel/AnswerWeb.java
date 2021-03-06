/******************************************************************************
 * e-voting system                                                            *
 * Copyright (C) 2016 DSX Technologies Limited.                               *
 *                                                                            *
 * This program is free software; you can redistribute it and/or modify       *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation; either version 2 of the License, or          *
 * (at your option) any later version.                                        *
 *                                                                            *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied                         *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 *                                                                            *
 * You can find copy of the GNU General Public License in LICENSE.txt file    *
 * at the top-level directory of this distribution.                           *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

package uk.dsxt.voting.client.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;
import uk.dsxt.voting.common.domain.dataModel.Answer;

import java.math.BigDecimal;

@Value
public class AnswerWeb {
    String id;
    String name;
    BigDecimal votes;

    @JsonCreator
    public AnswerWeb(@JsonProperty("id") String id, @JsonProperty("name") String name, @JsonProperty("votes") BigDecimal votes) {
        this.id = id;
        this.name = name;
        this.votes = votes;
    }

    public AnswerWeb(Answer a) {
        this.id = a.getId();
        this.name = a.getName();
        this.votes = null;
    }
    
    public AnswerWeb(Answer a, BigDecimal voteAmount) {
        this.id = a.getId();
        this.name = a.getName();
        this.votes = voteAmount;
    }
}
