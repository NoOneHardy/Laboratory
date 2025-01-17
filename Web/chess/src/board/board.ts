export interface Board {
  rows: {
    fields: {
      piece: string | null
    }[]
  }[]
}