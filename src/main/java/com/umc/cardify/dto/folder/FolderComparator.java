package com.umc.cardify.dto.folder;

import com.umc.cardify.domain.Folder;
import com.umc.cardify.domain.enums.MarkStatus;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FolderComparator implements Comparator<Folder> {
    private static final Pattern pattern = Pattern.compile("(\\D*)(\\d*)");
    private final String order;

    public FolderComparator(String order) {
        this.order = order;
    }

    @Override
    public int compare(Folder f1, Folder f2) {
        // markState가 ACTIVE인 것을 상단으로 고정하는 코드(1순위)
        if (f1.getMarkState() == MarkStatus.ACTIVE && f2.getMarkState() != MarkStatus.ACTIVE) {
            return -1;
        } else if (f1.getMarkState() != MarkStatus.ACTIVE && f2.getMarkState() == MarkStatus.ACTIVE) {
            return 1;
        } else if (f1.getMarkState() == MarkStatus.ACTIVE && f2.getMarkState() == MarkStatus.ACTIVE) {
            return f1.getMarkDate().compareTo(f2.getMarkDate());
        }

        // 날짜 기준 정렬
        if (order.equals("edit-oldest") || order.equals("edit-newest")) {
            int dateCompare = f1.getEditDate().compareTo(f2.getEditDate());
            return order.equals("edit-oldest") ? dateCompare : -dateCompare;
        }

        // 이름 기준 정렬
        boolean isDescending = order.equals("desc");
        String name1 = f1.getName();
        String name2 = f2.getName();

        Matcher m1 = pattern.matcher(name1);
        Matcher m2 = pattern.matcher(name2);

        while (m1.find() && m2.find()) {
            int nonDigitCompare = m1.group(1).compareTo(m2.group(1));
            if (nonDigitCompare != 0) {
                return isDescending ? -nonDigitCompare : nonDigitCompare;
            }

            if (m1.group(2).isEmpty() && m2.group(2).isEmpty()) {
                continue;
            } else if (m1.group(2).isEmpty()) {
                return isDescending ? 1 : -1;
            } else if (m2.group(2).isEmpty()) {
                return isDescending ? -1 : 1;
            }

            int num1 = Integer.parseInt(m1.group(2));
            int num2 = Integer.parseInt(m2.group(2));

            if (num1 != num2) {
                return isDescending ? Integer.compare(num2, num1) : Integer.compare(num1, num2);
            }
        }

        return isDescending ? name2.compareTo(name1) : name1.compareTo(name2);
    }
}