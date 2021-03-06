/*
 * Copyright (c) 2015-2017 Dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.panda_lang.panda.implementation.interpreter.extractor.prepared;

import org.panda_lang.framework.interpreter.extractor.Extractor;
import org.panda_lang.framework.interpreter.lexer.TokenReader;
import org.panda_lang.framework.interpreter.lexer.TokenRepresentation;
import org.panda_lang.framework.interpreter.lexer.TokenizedSource;
import org.panda_lang.panda.implementation.interpreter.extractor.TokenPattern;
import org.panda_lang.panda.implementation.interpreter.extractor.TokenPatternUnit;
import org.panda_lang.panda.implementation.interpreter.lexer.PandaTokenizedSource;

import java.util.ArrayList;
import java.util.List;

public class PreparedExtractor implements Extractor {

    private final TokenPattern pattern;
    private final List<TokenizedSource> gaps;

    public PreparedExtractor(TokenPattern pattern) {
        this.pattern = pattern;
        this.gaps = new ArrayList<>();
    }

    @Override
    public List<TokenizedSource> extract(TokenReader tokenReader) {
        TokenPatternUnit[] units = pattern.getUnits();
        TokenizedSource tokenizedSource = tokenReader.getTokenizedSource();
        PreparedSource preparedSource = new PreparedSource(tokenizedSource);

        int hardTypedUnits = PreparedSourceUtils.countHardTypedUnits(units);
        int[] positions = new int[hardTypedUnits];
        int[] indexes = new int[hardTypedUnits];

        for (int i = 0, j = 0; i < units.length; i++) {
            TokenPatternUnit unit = units[i];

            if (unit.isGap()) {
                continue;
            }

            int lastIndexOfUnit = PreparedSourceUtils.lastIndexOf(preparedSource, unit);

            if (lastIndexOfUnit == -1) {
                return null;
            }

            int index = j++;
            indexes[index] = i;
            positions[index] = lastIndexOfUnit;
        }

        for (int i = 0, previousIndex = -1; i < positions.length; i++) {
            int index = positions[i];

            if (index > previousIndex) {
                previousIndex = index;
                continue;
            }

            return null;
        }

        for (int i = 0; i < positions.length; i++) {
            tokenReader.synchronize();

            int currentIndex = indexes[i];
            int previousIndex = currentIndex - 1;
            int indexOfUnit = positions[i] - 1;

            if (currentIndex > 0 && currentIndex < units.length) {
                TokenPatternUnit unit = units[previousIndex];

                if (!unit.isGap()) {
                    tokenReader.read();
                    continue;
                }
            }

            TokenizedSource gap = new PandaTokenizedSource();

            for (TokenRepresentation representation : tokenReader) {
                int index = tokenReader.getIndex();

                if (index >= indexOfUnit) {
                    break;
                }

                tokenReader.read();
                gap.addToken(representation);
            }

            tokenReader.read();
            gaps.add(gap);
        }

        tokenReader.synchronize();

        if (!(tokenReader.getIndex() + 1 >= tokenReader.getTokenizedSource().size())) {
            return null;
        }

        return gaps;
    }

}
