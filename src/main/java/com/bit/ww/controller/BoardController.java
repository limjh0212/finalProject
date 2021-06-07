package com.bit.ww.controller;

import com.bit.ww.dto.BoardDTO;
import com.bit.ww.dto.CmntDTO;
import com.bit.ww.dto.PostDTO;
import com.bit.ww.entity.CmntEntity;
import com.bit.ww.entity.PostEntity;
import com.bit.ww.service.BoardService;
import com.bit.ww.service.CmntService;
import com.bit.ww.service.LikeService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/api/cmnty")
public class BoardController {
    private final BoardService boardService;
    private final CmntService cmntService;
    private final LikeService likeService;

    @ApiOperation(value = "게시판 정보 조회", notes = "게시판 정보 조회")
    @GetMapping("/{boardname}")
    public BoardDTO boardInfo(@PathVariable String boardname){
        return boardService.findBoard(boardname);
    }

    @ApiOperation(value = "게시판 글 리스트", notes = "게시판 글 리스트")
    @GetMapping("/{boardname}/posts")
    public HashMap posts(@PathVariable String boardname){
        HashMap<String, Object> posts = new HashMap<>();
        posts.put("posts", boardService.findPosts(boardname));
        posts.put("cntPosts", boardService.cntPosts(boardname));
        return posts;
    }

    @ApiOperation(value = "게시판 글 작성", notes = "게시판 글 작성")
    @PostMapping("/{boardname}")
    public PostEntity post(@PathVariable String boardname, @Validated @RequestBody PostDTO postDTO){
        BoardDTO boardDTO = boardService.findBoard(boardname);
        int lastnum = boardDTO.getLastnum()+1;
        postDTO.setBoardname(boardname);
        postDTO.setPostnum(lastnum);
        boardDTO.setLastnum(lastnum);
        boardService.saveBoard(boardDTO);
        return boardService.savePost(postDTO);
    }

    @ApiOperation(value = "임시저장", notes = "임시저장")
    @PostMapping("/temp/{boardname}")
    public PostEntity temp(@PathVariable String boardname, @Validated @RequestBody PostDTO postDTO){
        BoardDTO boardDTO = boardService.findBoard("temp");
        int lastnum = boardDTO.getLastnum()+1;
        if (boardname.equals("freeBoard")){
            postDTO.setTempnum(1);
        }else if (boardname.equals("OOTD")){
            postDTO.setTempnum(2);
        }else if (boardname.equals("hotDeal")){
            postDTO.setTempnum(3);
        }else if (boardname.equals("qna")){
            postDTO.setTempnum(4);
        }
        postDTO.setPostnum(lastnum);
        postDTO.setBoardname("temp");
        postDTO.setIstemp(true);
        boardDTO.setLastnum(lastnum);
        boardService.saveBoard(boardDTO);
        return boardService.savePost(postDTO);
    }
    // 게시물 상세보기 Todo: 좋아요 여부 추가필요.
    @ApiOperation(value = "게시물 상세보기", notes = "게시물 상세보기")
    @GetMapping("/post/{postId}")
    public HashMap post(@PathVariable("postId") int num){
        HashMap<String, Object> post = new HashMap<>();
        PostDTO postDTO = boardService.getPost(num);
        int readcount = postDTO.getReadcount()+1;
        postDTO.setReadcount(readcount);
        boardService.savePost(postDTO);
        post.put("post", postDTO);
        int boardnum = postDTO.getBoardnum();
        //post.put("likecount", likeService.countLike(boardnum, num));

        List<CmntDTO> cmntList = cmntService.getCmntList(boardnum, num, 0);
        List<List<CmntDTO>> cmnt2List = new ArrayList<>();
        for(int i = 0; i<cmntList.size();i++){
            List<CmntDTO> temp = cmntService.getCmnt2List(boardnum,num,cmntList.get(i).getNum(),1);
            cmnt2List.add(temp);
        }
        post.put("cmntList", cmntList);
        post.put("cmnt2List", cmnt2List);
        return post;
    }

    @ApiOperation(value = "게시판 글 수정", notes = "게시판 글 수정")
    @PatchMapping("/post/{postId}")
    public PostEntity post(@PathVariable("postId") int num, @Validated @RequestBody PostDTO postDTO){
        PostDTO updatePost = boardService.getPost(num);
        updatePost.setEditdate(LocalDateTime.now());
        updatePost.setTitle(postDTO.getTitle());
        updatePost.setContent(postDTO.getContent());
        updatePost.setImg(postDTO.getImg());
        updatePost.setBoardname(postDTO.getBoardname());
        return boardService.savePost(updatePost);
    }

    @ApiOperation(value = "게시판 글 삭제", notes = "게시판 글 삭제")
    @DeleteMapping("/post/{postId}")
    public String deletePost(@PathVariable("postId") int num){
        boardService.deletePost(num);
        return "delete ok!";
    }
    // QnA 게시판
    @ApiOperation(value = "QnA 답변 작성", notes = "QnA 답변 작성")
    @PostMapping(value ="/answer/{postId}")
    public CmntEntity answer(@PathVariable("postId") int num, @RequestBody @Validated CmntDTO cmntDTO){
        PostDTO questionDTO = boardService.getPost(num);
        questionDTO.setIsanswered(true);
        boardService.savePost(questionDTO);
        cmntDTO.setBoardnum(4);
        cmntDTO.setPostnum(questionDTO.getPostnum());
        return cmntService.saveCmnt(cmntDTO);
    }
    // 댓글
    @ApiOperation(value = "댓글 등록", notes = "댓글 등록")
    @PostMapping(value ="/cmnt/post")
    public CmntEntity cmntPost(@RequestBody @Validated CmntDTO cmntDTO){
        return cmntService.saveCmnt(cmntDTO);
    }

    // Todo: 추후 view에서 boardnum, postnum, writer 가져오고, 댓글에서 대댓글 달기 클릭 시, 댓글의 num을 cmntnum으로 가져올 것.
    @ApiOperation(value = "대댓글 등록", notes = "대댓글 등록(댓글번호입력)")
    @PostMapping(value ="/cmnt/post/{cmntId}")
    public CmntEntity cmnt2Post(@PathVariable("cmntId") int cmntnum, @RequestBody @Validated CmntDTO cmntDTO){
        CmntDTO cmnt2DTO = cmntDTO;
        cmnt2DTO.setDepth(1);
        cmnt2DTO.setCmntnum(cmntnum);
        return cmntService.saveCmnt(cmnt2DTO);
    }

    @ApiOperation(value = "댓글 수정", notes = "댓글 수정")
    @PatchMapping("/cmnt/{cmntId}")
    public String updateCmnt(@PathVariable("cmntId") int num, @RequestBody @Validated CmntDTO cmntDTO){
        cmntDTO.setContent(cmntDTO.getContent());
        cmntService.saveCmnt(cmntDTO);
        return "update ok!";
    }

    @ApiOperation(value = "댓글 삭제(대댓글이 없을 경우)", notes = "댓글 삭제(대댓글이 없을 경우)")
    @DeleteMapping("/cmnt/{cmntId}")
    public String deleteCmnt(@PathVariable("cmntId") int num){
        cmntService.deleteCmnt(num);
        return "delete - deleteCmnt ok!";
    }

    // 댓글 삭제(대댓글이 있을 경우) - 값 변경
    @ApiOperation(value = "댓글 삭제(대댓글이 있을 경우)", notes = "댓글 삭제(대댓글이 있을 경우)")
    @PutMapping("/cmnt/{cmntId}")
    public String delete(@PathVariable("cmntId") int num, @RequestBody @Validated CmntDTO cmntDTO){
        CmntDTO delCmntDTO = cmntDTO;
        boolean existCmnt2 = cmntService.existCmnt2(delCmntDTO.getNum());
        if(existCmnt2 == true) {
            delCmntDTO.setWriter("deleted");
            delCmntDTO.setContent("deleted");
            cmntService.saveCmnt(delCmntDTO);
            return "delete - changeCmnt ok!";
        } else {
            return deleteCmnt(delCmntDTO.getNum());
        }
    }
}
