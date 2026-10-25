package com.example.quanlydetaisinhvien.service.mail;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;
class ResultSubjectBoundaryTests {
    @ParameterizedTest @ValueSource(ints={0,1,166,167,168,199,200,255})
    void subjectFitsTheUnchangedDatabaseColumn(int size) {
        String topic="Đ".repeat(size), full="Kết quả đề tài sinh viên HCMUTE - "+topic;
        var subject=KetquaEmailOutbox.subject(topic);
        assertThat(subject.length()).isLessThanOrEqualTo(200);
        if(full.length()<=200) assertThat(subject).isEqualTo(full);
        else assertThat(subject).endsWith("…");
    }
    @Test void topicWithSurrogatesAndLineBreaksHasNoBrokenUtf16OrHeaderInjection() {
        var subject=KetquaEmailOutbox.subject("Đề tài\r\n"+"😀".repeat(120));
        assertThat(subject).doesNotContain("\r","\n");
        for(int i=0;i<subject.length();i++) {
            if(Character.isHighSurrogate(subject.charAt(i))) {
                assertThat(i+1).isLessThan(subject.length());
                assertThat(Character.isLowSurrogate(subject.charAt(++i))).isTrue();
            } else assertThat(Character.isLowSurrogate(subject.charAt(i))).isFalse();
        }
    }
}
