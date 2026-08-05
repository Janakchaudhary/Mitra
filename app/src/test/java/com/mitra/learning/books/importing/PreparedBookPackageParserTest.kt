package com.mitra.learning.books.importing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreparedBookPackageParserTest {
    @Test
    fun parsesChatGptPreparedVocabularyAndQuestions() {
        val parsed = PreparedBookPackageParser.parse(
            """
            {
              "schemaVersion": 1,
              "preparedBy": "ChatGPT",
              "book": {
                "title": "Std 2 Gujarati",
                "subject": "Gujarati",
                "standard": 2,
                "language": "Gujarati",
                "pageCount": 27
              },
              "chapters": [{
                "key": "c1",
                "chapterNumber": 1,
                "titleGujarati": "પાઠ ૧",
                "startPage": 13,
                "endPage": 27,
                "pages": [{
                  "pageNumber": 15,
                  "summaryGujarati": "દંગોરો શબ્દ આવે છે.",
                  "visibleTextGujarati": "દંગોરો એટલે લાંબી લાકડી."
                }],
                "vocabulary": [{
                  "word": "દંગોરો",
                  "meaningGujarati": "લાંબી મજબૂત લાકડી",
                  "sourcePage": 15
                }],
                "concepts": [{
                  "key": "meaning",
                  "titleGujarati": "શબ્દ અર્થ",
                  "descriptionGujarati": "શબ્દોના અર્થ",
                  "sourcePageStart": 13,
                  "sourcePageEnd": 27,
                  "questions": [{
                    "promptGujarati": "દંગોરોનો અર્થ શું?",
                    "evaluationMode": "KEYWORD",
                    "expectedText": "લાંબી લાકડી",
                    "acceptedAnswers": ["લાકડી"],
                    "sourcePage": 15
                  }]
                }]
              }]
            }
            """.trimIndent()
        )

        assertEquals(13, parsed.chapters.single().startPage)
        assertEquals("દંગોરો", parsed.chapters.single().vocabulary.single().word)
        assertEquals(1, parsed.chapters.single().concepts.single().questions.size)
        assertTrue(parsed.book.pageCount >= parsed.chapters.single().endPage)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsPrintedPageOutsidePhysicalChapterRange() {
        PreparedBookPackageParser.parse(
            """
            {
              "schemaVersion": 1,
              "book": {"title":"Book","subject":"Gujarati","pageCount":20},
              "chapters": [{
                "key":"c1","titleGujarati":"પાઠ","startPage":13,"endPage":20,
                "pages":[{"pageNumber":1,"summaryGujarati":"wrong physical page"}]
              }]
            }
            """.trimIndent()
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOverlappingPhysicalChapterRanges() {
        PreparedBookPackageParser.parse(
            """
            {
              "schemaVersion": 1,
              "book": {"title":"Book","subject":"Gujarati","pageCount":30},
              "chapters": [
                {"key":"c1","titleGujarati":"પાઠ ૧","startPage":10,"endPage":20},
                {"key":"c2","titleGujarati":"પાઠ ૨","startPage":20,"endPage":30}
              ]
            }
            """.trimIndent()
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsQuestionSourcePageOutsideChapter() {
        PreparedBookPackageParser.parse(
            """
            {
              "schemaVersion": 1,
              "book": {"title":"Book","subject":"Gujarati","pageCount":20},
              "chapters": [{
                "key":"c1","titleGujarati":"પાઠ","startPage":13,"endPage":20,
                "concepts":[{
                  "key":"meaning","titleGujarati":"અર્થ",
                  "sourcePageStart":13,"sourcePageEnd":20,
                  "questions":[{"promptGujarati":"અર્થ શું?","sourcePage":1}]
                }]
              }]
            }
            """.trimIndent()
        )
    }

}
