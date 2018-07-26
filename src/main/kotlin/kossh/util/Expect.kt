package kossh.util

data class Expect(val expectWhen: (String)->Boolean, val send: String)