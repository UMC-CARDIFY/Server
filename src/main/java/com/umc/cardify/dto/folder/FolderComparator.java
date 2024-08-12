package com.umc.cardify.dto.folder;

import com.umc.cardify.domain.Folder;
import com.umc.cardify.domain.enums.MarkStatus;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FolderComparator implements Comparator<Folder> {
    private static final Pattern pattern = Pattern.compile("(\\D*)(\\d*)");

    @Override
    public int compare(Folder f1, Folder f2) {
        // markState가 ACTIVE인 것을 상단으로 고정하는 코드(없으면 쿼리 안돌아감)
        if (f1.getMarkState() == MarkStatus.ACTIVE && f2.getMarkState() != MarkStatus.ACTIVE) {
            return -1;
        } else if (f1.getMarkState() != MarkStatus.ACTIVE && f2.getMarkState() == MarkStatus.ACTIVE) {
            return 1;
        } else if (f1.getMarkState() == MarkStatus.ACTIVE && f2.getMarkState() == MarkStatus.ACTIVE) {
            return 0;
        }

        String name1 = f1.getName();
        String name2 = f2.getName();

        Matcher m1 = pattern.matcher(name1);
        Matcher m2 = pattern.matcher(name2);

        while (m1.find() && m2.find()) {
            int nonDigitCompare = m1.group(1).compareTo(m2.group(1));
            if (nonDigitCompare != 0) {
                return nonDigitCompare;
            }

            if (m1.group(2).isEmpty() && m2.group(2).isEmpty()) {
                continue;
            } else if (m1.group(2).isEmpty()) {
                return -1;
            } else if (m2.group(2).isEmpty()) {
                return 1;
            }

            int num1 = Integer.parseInt(m1.group(2));
            int num2 = Integer.parseInt(m2.group(2));

            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }

        return name1.compareTo(name2);
    }
}