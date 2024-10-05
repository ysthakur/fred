
int fn$fact (int i) {
  
  
  int var$443348f9;
  if (i == 0) {
    
    var$443348f9 = 1;
  } else {
  
    var$443348f9 = i * fn$fact(i - 1);
  }
  int var$34bb9ab4 = var$443348f9;

  return var$34bb9ab4;
}